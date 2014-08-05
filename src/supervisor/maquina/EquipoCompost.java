package supervisor.maquina;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class EquipoCompost extends GruaCnc {
	Boolean routine=false;
	Double[] entryPoint;
	Double[][] mixSteps;
	Double[] exitPoint;
	
	
	public OnMixingRoutineListener mixingRoutineListener;
	
	public void setOnMixingRoutineListener(OnMixingRoutineListener rtLst){
		mixingRoutineListener=rtLst;
	}
	
	public interface OnMixingRoutineListener {
		
		public void OnMixingStarts();
		public void OnMixingSucces();
		public void OnMixingFails();

	}
	
	
	public EquipoCompost(Double x_0, Double y_0, Double z_0, Double l_a,
			Double r_a, Double d_e, Double r_x, Double r_y, Double r_z,
			Double r_r, Double r_p, Double r_v, Double x_max, Double x_j,
			Double y_j, Double x_min, Double y_max, Double y_min, Double z_max,
			Double z_min, Double dr, Double x_v1, Double y_v1, Double ar,
			Double ai, Double lr, Double lv_r, Double derX, Double derY,
			String ip, short port) {
		super(x_0, y_0, z_0, l_a, r_a, d_e, r_x, r_y, r_z, r_r, r_p, r_v,
				x_max, x_j, y_j, x_min, y_max, y_min, z_max, z_min, dr, x_v1,
				y_v1, ar, ai, lr, lv_r, derX, derY, ip, port);
		super.setOnRoutineListener(new OnRoutineListener() {
			
			@Override
			public void OnToolStopFails() {
				// TODO Auto-generated method stub
				System.out.println("Error al  detener Herramienta");
			}
			
			@Override
			public void OnToolStop() {
				// TODO Auto-generated method stub
				System.out.println("Herramienta detenida");
			}
			
			@Override
			public void OnToolSpinFails() {
				// TODO Auto-generated method stub
				routine=false;
				mixingRoutineListener.OnMixingFails();
				System.out.println("Fallo al encender la herramienta");
			}
			
			@Override
			public void OnToolSpin() {
				// TODO Auto-generated method stub
				System.out.println("Herramienta operando");
				if(routine){
					insrtTool();
				}
			}
			
			@Override
			public void OnRetractToolSuccess() {
				// TODO Auto-generated method stub
				System.out.println("Extraccion de herramienta finalizada");
				routine=false;
				mixingRoutineListener.OnMixingSucces();
				
			}
			
			@Override
			public void OnRetractToolStarts() {
				// TODO Auto-generated method stub
				System.out.println("Inicia extracción de herramienta");
			}
			
			@Override
			public void OnRetractToolFails() {
				// TODO Auto-generated method stub
				routine=false;
				mixingRoutineListener.OnMixingFails();
			}
			
			@Override
			public void OnMixingSucces() {
				// TODO Auto-generated method stub
				System.out.println("Mezclado concluido");
				rtrtTool();
			}
			
			@Override
			public void OnMixingStarts() {
				// TODO Auto-generated method stub
				System.out.println("Inicia Mezclado");
				
			}
			
			@Override
			public void OnMixingFails() {
				// TODO Auto-generated method stub
				routine=false;
				mixingRoutineListener.OnMixingFails();
			}
			
			@Override
			public void OnInsertToolSucces() {
				// TODO Auto-generated method stub
				System.out.println("Herramienta introducida dentro del reactor");
				if(routine){
					mix(mixSteps);
				}
			}
			
			@Override
			public void OnInsertToolStarts() {
				// TODO Auto-generated method stub
				System.out.println("Inicia Introduccion de herramienta");
			}
			
			@Override
			public void OnInsertToolFails() {
				// TODO Auto-generated method stub
				routine=false;
				mixingRoutineListener.OnMixingFails();
			}
			
			@Override
			public void OnGtiSucces() {
				// TODO Auto-generated method stub
				System.out.println("herramienta en punto de insercion");
				if(routine){
					System.out.println("Encendiendo Herramienta");
					toolOn();
				}
			}
			
			@Override
			public void OnGtiStarts() {
				// TODO Auto-generated method stub

			}
			
			@Override
			public void OnGtiFails() {
				// TODO Auto-generated method stub
				routine=false;
				mixingRoutineListener.OnMixingFails();
			}
		});
	}
	
	public void mixingRoutine(int r,int s,String path){
		if(!ocupado.get()){
			BufferedReader inputStream=null;
			
			try {
				String[] linea;
				String l;
				List<String> pMezclado=new ArrayList<String>();
				
				inputStream=new BufferedReader(new FileReader(path));
				while((l=inputStream.readLine())!=null){
					pMezclado.add(l);
				}
				inputStream.close();
				
				Double[] maxRel=gestpos.getMaxCordRel();
				//punto de ingreso primera linea del archivo formato: y=<valor>
				linea=pMezclado.get(0).split("=");
				Double yin=maxRel[1]*(Double.parseDouble(linea[1])/1000.0);
				if(yin==0)
					yin=0.2;
				if(yin>=maxRel[1]-0.1)
					yin=maxRel[1]-0.2;
				entryPoint=new Double[]{0.1,yin};
				//punto de salida ultima linea del archivo formato: y=<valor>;
				linea=pMezclado.get(pMezclado.size()-1).split(";");
				linea=linea[0].split("=");
				if(linea[1]!=null){
					Double yout=maxRel[1]*(Double.parseDouble(linea[1])/1000.0);
					if(yout==0)
						yout=0.2;
					if(yout>=maxRel[1]-0.1)
						yout=maxRel[1]-0.2;
					exitPoint=new Double[]{0.1,yout};
				}
				else{
					//TODO Reportar archivo con mal formato y cancelar mezclado o asignar punto de salida arbitrario
				}
				//rutina de mezclado

				 /* formato:
				 *         | x | y | z | a |
				 * 	punto1 | x1| y1| z1| a1|
				 *  punto2 | x2| y2| z2| a2|
				 *     .
				 *     .
				 *     .
				 *  punton | xn| yn| zn| an|
				 *  coord[punto][coordenada]
				 */
				mixSteps=new Double[pMezclado.size()-2][4]; // se le quitan dos puntos que son el de inicio y el de final
				for(int i=1;pMezclado.size()>=(i+2);i++){
					String[] punto=pMezclado.get(i).split("=");
					//formato
					//0    1    2    3    4    5    
					//x=<valor>=y=<valor>=r=<valor>
					
					
					Double xrut=maxRel[0]*(Double.parseDouble(punto[1])/1000.0);
					if(xrut>=maxRel[0]-0.4)
						xrut=maxRel[0]-0.4;
					if(xrut<=0.1)
						xrut=0.2;
					mixSteps[i-1][0]=xrut;
					Double yrut=maxRel[1]*(Double.parseDouble(punto[3])/1000.0);
					if(yrut>=maxRel[1]-0.4)
						yrut=maxRel[1]-0.4;
					if(yrut<=0.1)
						yrut=0.2;
					mixSteps[i-1][1]=yrut;
					//calculando altura en z
					//TODO calcular de manera mas precisa la pendiente luego
					//pendiente =10/170=0.06
					mixSteps[i-1][2]=xrut*0.06;
					mixSteps[i-1][3]=Double.parseDouble(punto[5]);
					
				}
				System.out.println("Datos importados : ");
				System.out.println("Punto de ingreso : "+entryPoint[0]+";"+entryPoint[1]);
				System.out.println("Rutina : ");
				for(Double[] pnt:mixSteps){
					String salida="";
					for(Double cord:pnt){
						salida+=cord.toString()+" | ";
					}
					System.out.println(salida);
				}
				System.out.println("Punto de salida : "+exitPoint[0]+";"+exitPoint[1]);
				routine=true;
				gotoInsrtPoint(r, s, entryPoint[0],entryPoint[1]);
				mixingRoutineListener.OnMixingStarts();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}
	
}
