/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package supervisor.maquina;

import Bases.GestPos;
import Bases.cnc;

import java.text.DecimalFormat;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.omg.CORBA.Bounds;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.util.Duration;

/**
 *
 * @author Francisco
 */
public class GruaCnc {
	// Eventos
	// Coneccion al dispositivo
	OnConnectionListener conListener;
	
	OnRoutineListener routineListener;
	
	public interface OnRoutineListener {
		
		public void OnGtiStarts();
		public void OnGtiSucces();
		public void OnGtiFails();
		
		public void OnInsertToolStarts();
		public void OnInsertToolSucces();
		public void OnInsertToolFails();
		
		public void OnRetractToolStarts();
		public void OnRetractToolSuccess();
		public void OnRetractToolFails();
		
		public void OnMixingStarts();
		public void OnMixingSucces();
		public void OnMixingFails();
		
		public void OnToolSpin();
		public void OnToolStop();
		public void OnToolSpinFails();
		public void OnToolStopFails();
		
	}
	
	public void setOnRoutineListener(OnRoutineListener rtLst){
		routineListener=rtLst;
	}


	public interface OnConnectionListener {
		public void OnConnectionSucces();

		public void OnConnectionExistsErr();

		public void OnConnectionMaxLimitErr();

		public void OnConnectionSocketErr();

		public void OnConnectionTcpErr();
	}

	public void setOnConnectionListener(OnConnectionListener cmdLst) {
		conListener = cmdLst;
	}

	// Movimiento
	OnMovementListener movListener;

	public interface OnMovementListener {
		public void OnCommandFailed(String fuente);
		public void OnMovementSucces(int mov);
		public void OnPositionUpdate(Double x, Double y, Double z, Double r);
		public void OnDeviceIsBussy();
		public void OnOutOfBounds(); 
	}

	public void setOnOnMovementListener(OnMovementListener mvLst) {
		movListener = mvLst;
	}

	// Variable de almacenamiento de tipo de movimiento
	private int mov = 99;
	// Constantes de tipo de movimiento
	public static final int FREEMOVE = 0;
	public static final int GOTO_R_TOOL = 1;
	public static final int GOTO_R_JAV = 2;
	public static final int MIX = 3;
	public static final int INSRT_TOOL = 4;
	public static final int RTRT_TOOL = 5;
	public static final int INSRT_JAV = 6;
	public static final int RTRT_JAV = 7;
	public static final int MOV_XY_TOOL = 8;
	public static final int MOV_XY_JAV = 9;
	public static final int GOTO_INSRT = 10;

	// Clase de control del equipo
	// de momento la dll se inicializara en la clase principal luego se vera si
	// trasladar eso a esta clase

	cnc control = new cnc();
	// constante para codigos de error
	/*
	 * Codigos de error para la funcion de coneccion 0-->no error 1-->existe la
	 * coneccion 2-->maximo numero de conecciones alcanzado 3-->errror de socket
	 * 4-->fallo en la coneccion TCP
	 */
	public final int COM_NO_ERR = 0;
	public final int COM_ERR_CON_EXISTS = 1;
	public final int COM_ERR_MAX_CON = 2;
	public final int COM_ERR_SOCKET = 3;
	public final int COM_ERR_TCP = 4;

	/*
	 * Codigos de error para los demas parametros -1-->fallo al enviar
	 * 0-->correcto 1-->codigo de funcion incorrecto o no soportado
	 * 2-->direccion invalida o no doportada 3-->datos invalidos o no soportados
	 * 4-->fallo en la ejecuci贸n del movimiento 5-->movimiento en ejecuci贸n(la
	 * operacion puede esperar) 6-->el equipo esta ocupado no puede ejecutar la
	 * orden de momento 8-->error de comprobacion de archivo 10-->ruta a puerta
	 * de acceso invalida 11-->el dispoditivo objetivo no responde 224-->error
	 * de transmicion o marco de datos de modbus invalido 255-->equipo tarda
	 * mucho en responder(dispositivo no responde o no esta conectado a la red)
	 * 225-->Movimiento no definido 226-->fallo en la apertura del archivo
	 * 227-->Error de direccion de base
	 */
	public final int NO_ERR = 0;
	public final int MODBUS_SND_FAIL = -1;
	public final int MODBUS_INVALID_FUNC = 1;
	public final int MODBUS_INVALID_ADDR = 2;
	public final int MODBUS_INVALID_DATA = 3;
	public final int MODBUS_PERFORM_FAIL = 4;
	public final int MODBUS_DEVICE_ACK = 5;
	public final int MODBUS_DEVICE_BUSY = 6;
	public final int MODBUS_MEM_PARITY_ERR = 8;
	public final int MODBUS_INVALID_GATEWAY_PATH = 10;
	public final int MODBUS_DEVICE_NO_RESPOND = 11;
	public final int MODBUS_FRAME_ERR = 224;
	public final int MODBUS_TIMEOUT_ERR = 255;
	public final int MODBUS_PFM_UNDEFINED = 225;
	public final int MODBUS_OPEN_FILE_FAIL = 226;
	public final int MODBUS_BASEADDR_ERR = 227;
	// variables de comunicacion
	private final String ip;
	private final Short port;
	Task<Integer> task;
	Thread th;
	// Variables de estado del equipo
	public BooleanProperty ocupado = new SimpleBooleanProperty(false);
	// //////////////////////////////////////////////////////
	public SimpleBooleanProperty conected = new SimpleBooleanProperty(false);
	// Temporizadores de actualizacion de datos de posici贸n y estado
	Timeline actualizador = new Timeline(new KeyFrame(Duration.millis(1250),
			new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					// Temporizador para control de posicion
					// implementar funcion de actualizacion de datos
					actualizar();

					if (!ocupado.get()) {
						actualizador.stop();
					}
				}
			}));
	Timeline toolChk = new Timeline(new KeyFrame(Duration.millis(500),
			new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					conectar();
					System.out.println("Verificando estado de la herramienta");
					int resin=control.input_read((short)12);
					System.out.println("Estado :"+resin);
					
					if(tool){
			
						if(resin==0){
							routineListener.OnToolSpin();
							System.out.println("Herramienta Girando");
						}else{
							routineListener.OnToolSpinFails();
						}
					}else{
						if(resin==1){
							routineListener.OnToolStop();
							System.out.println("Herramienta detenida");
						}else{
							routineListener.OnToolStopFails();
						}
					}
					desconectar();
				}
			}));
	// Gestion de posicionamiento
	public GestPos gestpos;
	// Posicion de la punta del agitador
	public Double posX; // centimetros
	public Double posY; // centimetros
	public Double posZ; // centimetros
	public Double posR; // grados de inclinacion del agitador
	public Double nR_act; // numero de reactor actual
	public Double nR_des; // numero de reactor actual
	public Double sec; // sector del vagon actual
	// Posici髇 de la punta del agitador como propiedades para muestra en
	// interfaz de usuario
	public StringProperty show_posX = new SimpleStringProperty("Desconocido");
	public StringProperty show_posY = new SimpleStringProperty("Desconocido");
	public StringProperty show_posZ = new SimpleStringProperty("Desconocido");
	public StringProperty show_posR = new SimpleStringProperty("Desconocido");
	// Velocidades de los diferentes ejes
	public Double velX; // m/s
	public Double velY; // m/s
	public Double velZ; // m/s
	public Double velR; // grados/s
	// Posicion de la velocidad del agitador como propiedades para muestra en
	// interfaz de usuario
	public StringProperty show_velX = new SimpleStringProperty("Desconocido");
	public StringProperty show_velY = new SimpleStringProperty("Desconocido");
	public StringProperty show_velZ = new SimpleStringProperty("Desconocido");
	public StringProperty show_velR = new SimpleStringProperty("Desconocido");
	// Posicion de los diferentes elementos del equipo en metros o grados seg煤n
	// aplique
	public Double posPortico;
	public Double posCarro;
	public Double posElevador;
	public Double posRotador;
	public Double desFaseR = 0.0; // diferencia entre el angulo del controlador
									// y el angulo de la herramienta
	// Posicion de los diferentes ejes del equipo como propiedades para muestra
	// de datos
	public StringProperty show_posPortico = new SimpleStringProperty(
			"Desconocido");
	public StringProperty show_posCarro = new SimpleStringProperty(
			"Desconocido");
	public StringProperty show_posElevador = new SimpleStringProperty(
			"Desconocido");
	public StringProperty show_posRotador = new SimpleStringProperty(
			"Desconocido");
	// Posicion del equipo en pulsos
	public Integer pX;
	public Integer pY;
	public Integer pZ;
	public Integer pR;

	// Variables para manejo de ingreso y salida al reactor
	public Double aIn = 60.0; // angulo de ingreso de la herramienta
	//Variables para control de posicionamiento
	private Double posDesX=0.0;
	private Double posDesY=0.0;
	// Ubicacion del equipo en el reactor actual
	private Integer rActual = 0;
	private Integer sActual = 0;

	public SimpleStringProperty show_rActual = new SimpleStringProperty(
			"Desconocido");
	public SimpleStringProperty show_sActual = new SimpleStringProperty(
			"Desconocido");

	public Double xRel; 
	public Double yRel;

	public SimpleStringProperty show_xRel = new SimpleStringProperty(
			"Desconocido");
	public SimpleStringProperty show_yRel = new SimpleStringProperty(
			"Desconocido");

	// Constantes del equipo del equipo
	public final Double x0; // distancia entre el punto de origen y el eje de
							// inclinacion del cabezal en x
	public final Double y0; // distancia entre el punto de origen y el eje de
							// inclinacion del cabezal en y
	public final Double z0; // distancia entre el punto de origen y el eje de
							// inclinacion del cabezal en z
	public final Double la; // longitud del agitador
	public final Double ra; // radio agitador
	public final Double de; // distancia entre el eje de inclinacion de la
							// herramienta y el centro de la herramienta
	public final Double Rx; // Relacion revoluciones motor vs desplazamiento en
							// metros metros en x
	public final Double Ry; // Relacion revoluciones motor vs desplazamiento en
							// metros metros en y
	public final Double Rz; // Relacion revoluciones motor vs desplazamiento en
							// metros del elevador
	public final Double Rr; // Relacion revoluciones motor vs Grados actuador
	public final Double Rp; // Relacion Pulsos/Revoluci贸n
	public final Double Rv; // Relacion vmaquina
	public final Double xj; // distancia entre el punto de origen y la jabalina
							// en x
	public final Double yj; // distancia entre el punto de origen y la jabalina
							// en y

	// los siguientes valores son los maximos y minimos en las coordenadas del
	// controlador cnc
	public final Double zMax;
	public final Double zMin;
	public final Double xMax;
	public final Double xMin;
	public final Double yMax;
	public final Double yMin;

	// Constantes de posicion y dimenciones de cada vagon
	public final Double dr; // Distancia resgtringida al borde = es+ra
	public Double es; // espacio de seguridad
	public final Double x_v1;// distancia entre el primer reactor y el eje de
								// referencia en x
	public final Double y_v1;// distancia entre el primer reactor y el eje de
								// referencia en y
	public final Double ar; // ancho del reactor (interno)
	public final Double ai; // ancho del intercambiador de calor
	public final Double lr; // largo del reactor
	public final Double lv_r; // largo de la parte rectangular del vagon (antes
								// de llegar a la zona inclinada)
	public final Double derX;// distancia entre vagones en x
	public final Double derY;// distancia entre vagones en y
	
	
	public int error=0;
	
	public Boolean tool=false;
	// ///////////////////////////////////////////////////////////////////////////////
	//
	DecimalFormat df = new DecimalFormat("0.00");

	// Constructor, de momento solo se asegura de inicializar las constantes
	// propias del equipo relacionadas con su dimesionamiento
	public GruaCnc(Double x_0, Double y_0, Double z_0, Double l_a, Double r_a,
			Double d_e, Double r_x, Double r_y, Double r_z, Double r_r,
			Double r_p, Double r_v, Double x_max, Double x_j, Double y_j,
			Double x_min, Double y_max, Double y_min, Double z_max,
			Double z_min, Double dr, Double x_v1, Double y_v1, Double ar,
			Double ai, Double lr, // largo del reactor
			Double lv_r, // largo de la parte rectangular del vagon (antes de llegar a la zona inclinada)
			Double derX,// distancia entre vagones en x
			Double derY, String ip, short port) {

		control.init();
		this.x0 = x_0;
		this.y0 = y_0;
		this.z0 = z_0;
		this.la = l_a;
		this.ra = r_a;
		this.de = d_e;
		this.Rx = r_x;
		this.Ry = r_y;
		this.Rz = r_z;
		this.Rr = r_r;
		this.Rp = r_p;
		this.Rv = r_v;
		this.xMax = x_max;
		this.xMin = x_min;
		this.yMax = y_max;
		this.yMin = y_min;
		this.zMax = z_max;
		this.zMin = z_min;
		this.xj = x_j;
		this.yj = y_j;
		this.dr = dr;
		this.x_v1 = x_v1;
		this.y_v1 = y_v1;
		this.ar = ar;
		this.ai = ai;
		this.lr = lr;
		this.lv_r = lv_r;
		this.derX = derX;
		this.derY = derY;
		this.ip = ip;
		this.port = port;

		actualizador.setCycleCount(Timeline.INDEFINITE);
		toolChk.setCycleCount(0);
		gestpos = new GestPos(x_v1, y_v1, dr, lr, derX, derY, ai, ar);
	}

	// Metodos

	public void conectar_asinc() {

		task = new Task<Integer>() {
			@Override
			protected Integer call() throws Exception {
				int conect = control.modbus_connect(ip, port);
				System.out.println("Resultado Coneccion : " + conect);
				return conect;

			}

		};

		task.onSucceededProperty().addListener(new ChangeListener<Object>() {
			@Override
			public void changed(ObservableValue<?> ov, Object t, Object t1) {
				System.out.println("Fin del intento de coneccion :");
				actualizar();
				try {
					switch (task.get()) {
					case COM_NO_ERR: {
						conListener.OnConnectionSucces();
						break;
					}
					case COM_ERR_CON_EXISTS: {
						conListener.OnConnectionExistsErr();
						break;
					}
					case COM_ERR_MAX_CON: {
						conListener.OnConnectionMaxLimitErr();
						break;
					}
					case COM_ERR_SOCKET: {
						conListener.OnConnectionSocketErr();
						break;
					}
					case COM_ERR_TCP: {
						conListener.OnConnectionTcpErr();
						break;
					}

					}
				} catch (InterruptedException ex) {
					Logger.getLogger(GruaCnc.class.getName()).log(Level.SEVERE,
							null, ex);
				} catch (ExecutionException ex) {
					Logger.getLogger(GruaCnc.class.getName()).log(Level.SEVERE,
							null, ex);
				}
			}
		});

		th = new Thread(task);
		th.setDaemon(true);
		th.start();
	}

	public int conectar() {
		int conect = control.modbus_connect(ip, port);
		System.out.println("Resultado Coneccion : " + conect);
		return conect;
	}

	public void desconectar() {
		control.modbus_close();
	}

	public void detener() {
		conectar();
		control.stop();
		desconectar();
	}

	/**
	 * Funcion para movimientos libres del equipo (se comanda directamente al
	 * cnc no hay conversion a coordenadas de la punta de la herramienta)
	 * 
	 * @param x
	 *            posicion absoluta del eje x del equipo
	 * @param y
	 *            posicion absoluta del eje y del equipo
	 * @param z
	 *            posicion absoluta del eje z del equipo
	 * @param r
	 *            posicion absoluta del eje r del equipo
	 */
	public void free_move(Double x, Double y, Double z, Double r) {
		if (!ocupado.get()) {
			int pulsos1;
			int pulsos2;
			int pulsos3;
			//int pulsos4;//TODO volver  a habilitar z cuando se pueda
			int pulsos5;

			pulsos1 = (int) (x * Rx * Rp);
			pulsos2 = pulsos1;
			pulsos3 = (int) (y * Ry * Rp);
			//pulsos4 = (int) (z * Rz * Rp);
			pulsos5 = (int) (r * Rr * Rp);

			short[] ejes = { 1,
					2,
					3,
					4//,
					//5
					};
			int[] pulsos = { pulsos1,
					pulsos2,
					pulsos3,
					//pulsos4,
					pulsos5 };

			conectar();
			int res = control.move(ejes, pulsos, (short) 4);//5);
			desconectar();
			if (res == 0) {
				System.out.println("Comando enviado con exito");
				actualizador.play();
				ocupado.set(true);
				mov = FREEMOVE;
			} else {
				System.out
						.println("Fallo al enviar el comando: 'Movimiento Libre'");
				movListener
						.OnCommandFailed("Fallo al enviar el comando: 'Movimiento Libre'");
			}

		} else {
			movListener.OnDeviceIsBussy();
		}
	}

	/**
	 * Funci髇 de posicionamiento de la herramienta (angulo actual) en
	 * coordenadas relativas, al sector del reactor requerido.
	 * 
	 * @param r
	 *            Numero de reactor requerido
	 * @param s
	 *            Sector requerido
	 * @param x
	 *            Posicion de destino en X respecto a la referencia del sector
	 *            [cm]
	 * @param y
	 *            Posicion de destino en Y respecto a la referencia del sector
	 *            [cm]
	 */
	public void gotoRtool(int r, int s, Double x, Double y) {
		Double[] pref = gestpos.getRefPoint(r, s);
		Double[] p_abs = { x + pref[0], y + pref[1] };
		if ((r == rActual && s == sActual) || posR < 1) {
			try {
				gestpos.getReactor(p_abs[0], p_abs[1]);
				movXY_tool(p_abs[0], p_abs[1]);
				mov = GOTO_R_TOOL;
			} catch (Bounds e) {
				movListener.OnOutOfBounds();
				e.printStackTrace();
			}
		} else {
			System.out
					.println("Imposible Cambiar de Sector o Reactor sin replegar la herramienta");
			movListener.OnOutOfBounds();
		}

	}

	/**
	 * Funci髇 de posicionamiento de la herramienta (angulo requerido) en
	 * coordenadas relativas, al sector del reactor requerido.
	 * 
	 * @param r
	 *            Numero de reactor requerido
	 * @param s
	 *            Sector requerido
	 * @param x
	 *            Posicion de destino en X respecto a la referencia del sector
	 *            [cm]
	 * @param y
	 *            Posicion de destino en Y respecto a la referencia del sector
	 *            [cm]
	 * @param a
	 *            Angulo en el que se espera tener la herramienta
	 */
	public void gotoRtool(int r, int s, Double x, Double y, Double a) {
		Double[] pref = gestpos.getRefPoint(r, s);
		Double[] p_abs = { x + pref[0], y + pref[1] };
		if ((r == rActual && s == sActual) || posR < 1) {
			try {
				gestpos.getReactor(p_abs[0], p_abs[1]);
				movXY_tool(p_abs[0], p_abs[1], a);
				if(mov!=GOTO_INSRT)
					mov = GOTO_R_TOOL;
			} catch (Bounds e) {
				movListener.OnOutOfBounds();
				if(mov==GOTO_INSRT)
					routineListener.OnGtiFails();
				e.printStackTrace();
			}
		} else {
			System.out
					.println("Imposible Cambiar de Sector o Reactor sin replegar la herramienta");
			movListener.OnOutOfBounds();
			if(mov==GOTO_INSRT)
				routineListener.OnGtiFails();
		}

	}

	/**
	 * Funci髇 de posicionamiento de la jabalina en coordenadas relativas al
	 * sector del reactor requerido.
	 * 
	 * @param r
	 *            Numero de reactor requerido
	 * @param s
	 *            Sector requerido
	 * @param x
	 *            Posicion de destino en X respecto a la referencia del sector
	 *            [cm]
	 * @param y
	 *            Posicion de destino en Y respecto a la referencia del sector
	 *            [cm]
	 */
	public void gotoRjav(int r, int s, Double x, Double y) {
		Double[] pref = gestpos.getRefPoint(r, s);
		Double[] p_abs = { x + pref[0], y + pref[1] };
		try {
			gestpos.getReactor(p_abs[0], p_abs[1]);
			movXY_jav(p_abs[0], p_abs[1]);
			mov = GOTO_R_JAV;
		} catch (Bounds e) {
			movListener.OnOutOfBounds();
			e.printStackTrace();
		}

	}

	/**
	 * Funci髇 para avance con mezclado dentro del vagon y sector actuales, en
	 * coordenadas relativas al sector del reactor requerido
	 * @param coord matriz donde se almacenan las coordenadas relativas para el mezclado donde las filas son los puntos y las columnas las coordenadas
	 * 		coord[punto][coordenada]
	 * formato:
	 *         | x | y | z | a |
	 * 	punto1 | x1| y1| z1| a1|
	 *  punto2 | x2| y2| z2| a2|
	 *     .
	 *     .
	 *     .
	 *  punton | xn| yn| zn| an|
	 */
	
	public void mix(Double coord[][]) {
		if(error==0&&!ocupado.get()){
			Integer[] reacActual;
			Double[] max;
			Boolean fueraDeLimite=false;
			Double[] pref;
			try {//primero verificar si las coordenadas actuales de la punta corresponden a un reactor
				reacActual = gestpos.getReactor(posX, posY);
				max=gestpos.getMaxCord(reacActual[0],reacActual[1]);
				pref=gestpos.getRefPoint(reacActual[0],reacActual[1]);
				for(Double[] punto:coord){
					Double xabs=punto[0]+pref[0];
					Double yabs=punto[1]+pref[1];
					if((xabs<=max[0])&&(yabs<=max[1])&&//verificar que x e y se encuentren en el rango permitido del reactor
							(xabs>=pref[0])&&(yabs>=pref[1])&&
							(punto[3]>=0)&&(punto[3]<=91)){//verificar que r se encuentre en rango
						Double xmaq = xabs - x0 - de * Math.sin(Math.toRadians(punto[3])) - la * Math.cos(Math.toRadians(punto[3]));
						Double ymaq = yabs - y0;
						Double zmaq = zMin + punto[2];
						 if (!((xmaq <= xMax) && (xmaq >= xMin) && (ymaq >= yMin)
								 && (ymaq <= yMax) && (zmaq <= zMax)&&(zmaq>=zMin))) {
							 fueraDeLimite=true;
							 movListener.OnOutOfBounds();
							 System.out.println("fuera del rango del equipo");
						 }
					}else{
						fueraDeLimite=true;
						movListener.OnOutOfBounds();
						System.out.println("fuera del rango del reactor");
					}
				}
				if(!fueraDeLimite){
					for(Double[] punto:coord){
						int res=0;
						Double xabs=punto[0]+pref[0];
						Double yabs=punto[1]+pref[1];
						Double xmaq = xabs - x0 - de * Math.sin(Math.toRadians(punto[3])) - la
								 * Math.cos(Math.toRadians(punto[3]));
					    Double ymaq = yabs - y0;
					    Double zmaq = zMin + punto[2];
					    
					    posDesX=xmaq;//variables para verificacion de rutina
					    posDesY=ymaq;
					    
						 int pulsos1 = (int) (xmaq * Rx * Rp);
						 int pulsos2 = pulsos1;
						 int pulsos3 = (int) (ymaq * Ry * Rp);
						 //int pulsos4 = (int) (zmaq * Rz * Rp);//TODO volver a habilitar z
						 int pulsos5 = (int) (punto[3] * Rr * Rp);
						 
						 System.out.println("xrel= "+punto[0]+"yrel= "+punto[1]+"zrel= "+punto[2]+"ang= "+punto[3]);
						 System.out.println("xabs= "+xabs+"ymaq= "+yabs);
						 System.out.println("xmaq= "+xmaq+"ymaq= "+ymaq+"zmaq= "+zmaq+"ang= "+punto[3]);
						 if(error==0){		
							 conectar();
							 res += control.fifo(pulsos1,
									 pulsos2,
									 pulsos3,
									 //pulsos4,//TODO volver a habilitar z
							 pulsos5, (int) (Rp/60.0 * 1200));
							 desconectar(); 
						 }
						 System.out.println("res es: "+res+"y el largo del arreglo es: "+coord.length);
						 if(res==0){
							 if(!ocupado.get())
								 routineListener.OnMixingStarts();
							 ocupado.set(true);
							 mov=MIX;
							 actualizador.play();
						 }
						 else{
							 detener();
							 error=MIX;
							 routineListener.OnMixingFails();
							 movListener.OnCommandFailed("Fallo al enviar los comandos de movimiento");
						 }
					}
				}
				
			} catch (Bounds e) {
				movListener.OnOutOfBounds();
				e.printStackTrace();
				routineListener.OnMixingFails();
			}
		}

		

	}

	public void gotoInsrtPoint(int r, int s, Double x, Double y) {
		gotoRtool(r, s, x, y, aIn);
		mov=GOTO_INSRT;
		
	}
	


	/**
	 * Funci髇 para insertar la herramienta dentro del vagon y sector actuales,
	 * en las coordenadas actuales
	 */
	public void insrtTool() {
		if (posR < 1 && !ocupado.get()) {
			try {
				gestpos.getReactor(posX, posY);
				Double xmaq = posPortico;
				Double ymaq = posCarro;
				Double zmaq = zMax;
				//Double zmaq2 = zMin;
				Double rmaq1 = aIn;

				int pulsos1 = (int) (xmaq * Rx * Rp);
				int pulsos2 = pulsos1;
				int pulsos3 = (int) (ymaq * Ry * Rp);
				//int pulsos4 = (int) (zmaq * Rz * Rp);//TODO volver a habilitar z
				int pulsos5 = (int) (rmaq1 * Rr * Rp);
				//int pulsos4_2 = (int) (zmaq2 * Rz * Rp);//TODO volver a habilitar z

				if ((xmaq <= xMax) && (xmaq >= xMin) && (ymaq >= yMin)
						&& (ymaq <= yMax) && (zmaq <= zMax)) {

					conectar();
					int res = control.fifo(pulsos1,
							pulsos2,
							pulsos3,
							//pulsos4,//TODO volver a habilitar z
							pulsos5, (int) (1300 * Rp/60.0));
					int res1 = control.fifo(pulsos1,
							pulsos2,
							pulsos3,
							//pulsos4_2,//TODO volver a habilitar z
							pulsos5, (int) (1300 * Rp/60.0));

					desconectar();
					if (res == NO_ERR && res1 == NO_ERR) {
						ocupado.set(true);
						routineListener.OnInsertToolStarts();
						actualizador.play();
						mov = INSRT_TOOL;
						
					} else {
						movListener
								.OnCommandFailed("Fallo al enviar comando 'Insertar Herramienta'");
					}

				} else {
					System.out
							.println("Movimiento fuera del rango permitido del equipo");
					movListener.OnOutOfBounds();
				}
			} catch (Bounds e) {
				// TODO Auto-generated catch block
				System.out.println("Fuera de Rango");
				movListener.OnOutOfBounds();
				e.printStackTrace();
			}

		}
	}

	/**
	 * Funci髇 para retirar la herramienta del vagon y sector actuales, en las
	 * coordenadas actuales
	 */
	public void rtrtTool() {
		if (!ocupado.get()) {
			try {
				gestpos.getReactor(posX, posY);
				Double ymaq = posCarro;
				Double rmaq2 = 0.0;
				Double xmaq = posX - x0 - de * Math.sin(Math.toRadians(90.0))
						- la * Math.cos(Math.toRadians(90.0));
			
				int pulsos1 = (int) (xmaq * Rx * Rp);
				int pulsos2 = pulsos1;
				int pulsos3 = (int) (ymaq * Ry * Rp);
				int pulsos5_2 = (int) (rmaq2 * Rr * Rp);
				if ((xmaq <= xMax) && (xmaq >= xMin) && (ymaq >= yMin)
						&& (ymaq <= yMax) ) {
					conectar();
					int res1 = control.fifo(pulsos1,
							pulsos2,
							pulsos3,
							pulsos5_2, (int) (1200 * Rp/60.0));
					desconectar();
					if (res1 == NO_ERR) {
						ocupado.set(true);
						routineListener.OnRetractToolStarts();
						actualizador.play();
						mov = RTRT_TOOL;
					}

				} else {
					System.out
							.println("Movimiento fuera del rango permitido del equipo");
					routineListener.OnRetractToolFails();
					movListener.OnOutOfBounds();
				}
				
//				gestpos.getReactor(posX, posY);
//				Double ymaq = posCarro;
//				Double zmaq = zMax;
//				//Double zmaq2 = zMin;//TODO volver a habilitar z
//				Double rmaq1 = aIn;
//				Double rmaq2 = 0.0;
//
//				Double xmaq = posX - x0 - de * Math.sin(Math.toRadians(aIn))
//						- la * Math.cos(Math.toRadians(aIn));
//
//				int pulsos1 = (int) (xmaq * Rx * Rp);
//				int pulsos2 = pulsos1;
//				int pulsos3 = (int) (ymaq * Ry * Rp);
//				//int pulsos4 = (int) (zmaq * Rz * Rp);//TODO volver a habilitar z
//				int pulsos5 = (int) (rmaq1 * Rr * Rp);
//				//int pulsos4_2 = (int) (zmaq2 * Rz * Rp);//TODO volver a habilitar z
//				int pulsos5_2 = (int) (rmaq2 * Rr * Rp);
//				if ((xmaq <= xMax) && (xmaq >= xMin) && (ymaq >= yMin)
//						&& (ymaq <= yMax) && (zmaq <= zMax)) {
//					conectar();
//					int res2 = control.fifo(pulsos1,
//							pulsos2,
//							pulsos3,
//							//pulsos4_2,//TODO volver a habilitar z
//							pulsos5,
//							(int) (1200 * Rp/60.0));
//					int res = control.fifo(pulsos1,
//							pulsos2,
//							pulsos3,
//							//pulsos4,//TODO volver a habilitar z
//							pulsos5, (int) (1200 * 166.667));
//					int res1 = control.fifo(pulsos1,
//							pulsos2,
//							pulsos3,
//							//pulsos4,//TODO volver a habilitar z
//							pulsos5_2, (int) (1200 * Rp/60.0));
//					desconectar();
//					if (res == NO_ERR && res1 == NO_ERR && res2 == NO_ERR) {
//						ocupado.set(true);
//						routineListener.OnRetractToolStarts();
//						actualizador.play();
//						mov = RTRT_TOOL;
//					}
//
//				} else {
//					System.out
//							.println("Movimiento fuera del rango permitido del equipo");
//					routineListener.OnRetractToolFails();
//					movListener.OnOutOfBounds();
//				}
			} catch (Bounds e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println("Fuera de Rango");
				movListener.OnOutOfBounds();
				routineListener.OnRetractToolFails();
			}

		}
	}

	/**
	 * Funci髇 para insertar la jabalina dentro del vagon y sector actuales, en
	 * las coordenadas actuales
	 */
	public void insrtJav() {

	}

	/**
	 * Funci髇 para retirar la jabalina del vagon y sector actuales, en las
	 * coordenadas actuales
	 */
	public void rtrtJav() {

	}

	/**
	 * Funci髇 para colocar la punta del equipo(solo si esta en posicion
	 * horizontal) en la posicion requerida en el plano xy repecto a la
	 * referencia del equipo
	 * 
	 * @param x
	 *            posici髇 en X deseada
	 * @param y
	 *            posici髇 en Y deseada
	 */
	public void movXY_tool(Double x, Double y) {
		if (!ocupado.get()) {
			int pulsos1;
			int pulsos2;
			int pulsos3;

			Double xmaq = x - x0 - de * Math.sin(Math.toRadians(posR)) - la
					* Math.cos(Math.toRadians(posR));
			Double ymaq = y - y0;

			if ((xmaq <= xMax) && (xmaq >= xMin) && (ymaq >= yMin)
					&& (ymaq <= yMax)) {
				pulsos1 = (int) (xmaq * Rx * Rp);
				pulsos2 = pulsos1;
				pulsos3 = (int) (ymaq * Ry * Rp);
				conectar();
				short[] ejes = { 1, 2, 3 };
				int[] pulsos = { pulsos1, pulsos2, pulsos3 };
				int res = control.move(ejes, pulsos, (short) 3);
				desconectar();
				if (res == NO_ERR) {

					System.out.println("Comando enviado con exito");
					ocupado.set(true);
					actualizador.play();

				} else {
					System.out.println("Fallo : " + res);
					movListener
							.OnCommandFailed("Fallo al enviar el comando: 'Mover Herramienta");
				}

			} else {
				movListener.OnOutOfBounds();
			}
			if (mov != GOTO_R_TOOL)
				mov = MOV_XY_TOOL;

		} else {
			movListener.OnDeviceIsBussy();
		}
	}

	public void movXY_tool(Double x, Double y, Double a) {
		if (!ocupado.get()) {
			int pulsos1;
			int pulsos2;
			int pulsos3;

			Double xmaq = x - x0 - de * Math.sin(Math.toRadians(a)) - la
					* Math.cos(Math.toRadians(a));
			Double ymaq = y - y0;

			posDesX=xmaq;//variables para verificacion de rutina
		    posDesY=ymaq;
			
			if ((xmaq <= xMax) && (xmaq >= xMin) && (ymaq >= yMin)
					&& (ymaq <= yMax)) {
				conectar();
				pulsos1 = (int) (xmaq * Rx * Rp);
				pulsos2 = pulsos1;
				pulsos3 = (int) (ymaq * Ry * Rp);

				short[] ejes = { 1, 2, 3 };
				int[] pulsos = { pulsos1, pulsos2, pulsos3 };
				int res = control.move(ejes, pulsos, (short) 3);

				desconectar();

				if (res == NO_ERR) {
					if(mov==GOTO_INSRT)
						routineListener.OnGtiStarts();
					System.out.println("Comando enviado con exito");
					ocupado.set(true);
					actualizador.play();

				} else {
					if(mov==GOTO_INSRT)
						routineListener.OnGtiFails();
					System.out.println("Fallo : " + res);
					movListener
							.OnCommandFailed("Fallo al enviar el comando: 'Mover Herramienta");
				}

			} else {
				movListener.OnOutOfBounds();
				if(mov==GOTO_INSRT)
					routineListener.OnGtiFails();
			}
			if (mov != GOTO_R_TOOL&&mov!=GOTO_INSRT)
				mov = MOV_XY_TOOL;

		} else {
			movListener.OnDeviceIsBussy();
			routineListener.OnGtiFails();
		}
	}

	/**
	 * Funci髇 para colocar la punta de la jabalina(solo si esta retraida) en la
	 * posicion requerida en el plano xy repecto a la referencia del equipo
	 * 
	 * @param x
	 *            posici髇 en X deseada
	 * @param y
	 *            posici髇 en Y deseada
	 */
	public void movXY_jav(Double x, Double y) {
		if (!ocupado.get()) {
			
			int pulsos1;
			int pulsos2;
			int pulsos3;

			Double xmaq = x - xj;
			Double ymaq = y - yj;

			if ((xmaq <= xMax) && (xmaq >= xMin) && (ymaq >= yMin)
					&& (ymaq <= yMax)) {
				pulsos1 = (int) (xmaq * Rx * Rp);
				pulsos2 = pulsos1;
				pulsos3 = (int) (ymaq * Ry * Rp);

				conectar();
				short[] ejes = { 1, 2, 3 };
				int[] pulsos = { pulsos1, pulsos2, pulsos3 };
				int res = control.move(ejes, pulsos, (short) 3);
				desconectar();
				if (res == 0) {
					System.out.println("Comando enviado con exito");
					ocupado.set(true);
					actualizador.play();

				} else {
					System.out.println("Fallo : " + res);
				}
			} else {
				movListener.OnOutOfBounds();
			}
			if (mov != GOTO_R_JAV)
				mov = MOV_XY_JAV;
		} else {
			movListener.OnDeviceIsBussy();
		}

	}

	void configX(Double vi, Double vm, Double ac) {

		short[] axis = new short[] { 1, 2 };
		int isp = (int) (vi * (Rp/60.0));
		int sp = (int) (vm * (Rp/60.0));
		int aacc = (int) (ac * (Rp/60.0));
		int[] init_speed = new int[] { isp, isp };
		int[] speed = new int[] { sp, sp };
		int[] acc = new int[] { aacc, aacc };
		int[] pmm = new int[] { 25, 25 };
		short num = 2;
		conectar();
		control.mov_parameter(axis, init_speed, speed, acc, pmm, num);
		desconectar();
	}

	void configY(Double vi, Double vm, Double ac) {
		conectar();
		control.mov_parameter((short) 3, (int) (vi * (Rp/60.0)),
				(int) (vm * (Rp/60.0)), (int) (ac * (Rp/60.0)), 2500);
		desconectar();
	}

	void configR(Double vi, Double vm, Double ac) {//TODO este es el configurador de Z corregir
		conectar();
		control.mov_parameter((short) 4, (int) (vi * (Rp/60.0)),
				(int) (vm * (Rp/60.0)), (int) (ac * (Rp/60.0)), 2500);
		desconectar();
	}

	void configZ(Double vi, Double vm, Double ac) {//TODO este es el configurador de R corregir
		conectar();
		control.mov_parameter((short) 5, (int) (vi * (Rp/60.0)),
				(int) (vm * (Rp/60.0)), (int) (ac * (Rp/60.0)), 2500);
		desconectar();
	}

	void configAll(Double vi, Double vm, Double ac) {
		conectar();
		short[] axis = new short[] { 1, 2, 3, 4, 5 };
		int isp = (int) (vi * 166.6667);
		int sp = (int) (vm * 166.6667);
		int aacc = (int) (ac * 166.6667);
		int[] init_speed = new int[] { isp, isp, isp, isp, isp };
		int[] speed = new int[] { sp, sp, sp, sp, sp };
		int[] acc = new int[] { aacc, aacc, aacc, aacc, aacc };
		int[] pmm = new int[] { 25, 25, 25, 25, 25 };
		short num = 5;
		control.mov_parameter(axis, init_speed, speed, acc, pmm, num);
		desconectar();
	}

	public void toolOn() {
		conectar();
		control.output_on((short) 0);
		tool=true;
		toolChk.play();
		desconectar();
	}

	public void toolOff() {
		conectar();
		control.output_off((short) 0);
		tool=false;
		toolChk.play();
		desconectar();
	}

	// ////////////////////////
	// Supervisor de movimiento
	private void actualizar() {

		conectar();

		int res = control.is_working();
		if (res != 2) {
			if (res == 1) {
				ocupado.set(true);
			} else {
				ocupado.set(false);
			}
		}
		if (control.get_axis_pos() == NO_ERR) {
			int pulsos1 = control.get_pos1();
			int pulsos2 = control.get_pos2();
			int pulsos3 = control.get_pos3();
			//int pulsos4 = control.get_pos4();//TODO rehablitar lector de valor de z
			int pulsos5 = control.get_pos4();//TODO tiene que ser pos5 para actualizar 5
			

			if (Math.abs(pulsos1 - pulsos2) > 300) {// se usa para garantizar
													// que no haya un desfase
													// grande entre los dos
													// motores del eje x
				control.stop(); //TODO Verificar que no traiga problemas
				System.out.println();
				System.out.println("Desfase entre motores eje X :"
						+ Math.abs(pulsos1 - pulsos2));
			}

			pX = -pulsos1;
			pY = -pulsos3;
			pZ =0;// -pulsos4;
			pR = -pulsos5;

			posPortico = pX.doubleValue() / (Rx * Rp*10000.0/Rp);//TODO verificar el tema electronica gear y la cantidad de pulsos por revolucion del encoder
			posCarro = pY.doubleValue() / (Ry * Rp*10000.0/Rp);
			posElevador = pZ.doubleValue() / (Rz * Rp*10000.0/Rp);
			posRotador = pR.doubleValue() / (Rr * Rp*10000.0/Rp);

			posR = posRotador;//TODO hacer posible la implementacion de un angulo inicial como variable
			
			posX = posPortico + x0 + de * Math.sin(Math.toRadians(posR)) + la
					* Math.cos(Math.toRadians(posR));
			posY = posCarro + y0;
			posZ = posElevador + z0 + de * Math.cos(Math.toRadians(posR)) - la
					* Math.sin(Math.toRadians(posR));

			try {
				Integer[] reactor_act = gestpos.getReactor(posX, posY);
				rActual = reactor_act[0];
				sActual = reactor_act[1];
				show_rActual.set(rActual.toString());
				show_sActual.set(sActual.toString());
				Double[] prefAct = gestpos.getRefPoint(rActual, sActual);
				xRel = posX - prefAct[0];
				yRel = posY - prefAct[1];
				show_xRel.set(xRel.toString());
				show_yRel.set(yRel.toString());

			} catch (Bounds e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
				if(mov==MIX){//TODO verificar si no asesina a cada rato las rutinas de mezclado
					detener();
					toolOff();
					routineListener.OnMixingFails();
				}
//				if(mov==RTRT_TOOL){//TODO verificar si no asesina a cada rato al paso de extraccion
//					detener();
//					toolOff();
//					routineListener.OnRetractToolFails();
//				}
			}

			movListener.OnPositionUpdate(posPortico, posCarro, posElevador,	posRotador);

			show_posX.set(df.format(posX));
			show_posY.set(df.format(posY));
			show_posZ.set(df.format(posZ));
			show_posR.set(df.format(posR));

			show_posPortico.set(df.format(posPortico));
			show_posCarro.set(df.format(posCarro));
			show_posElevador.set(df.format(posElevador));
			show_posRotador.set(df.format(posRotador));

			// actualizar estado del equipo, si libre u ocupado

			int reswk;
			reswk = control.is_working();
			if (reswk != 2) {
				if (reswk == 1) {
					ocupado.set(true);
				} else {
					ocupado.set(false);
					movListener.OnMovementSucces(mov);
					//TODO Verificar que los movimientos hayan concluido de manera adecuada	
					switch(mov){
					
					case(FREEMOVE):{
						
						break;
					}
					case(GOTO_R_TOOL):{
						
						break;
					}
					case(GOTO_R_JAV):{
						
						break;
					}
					case(MIX):{
						if((posCarro-posDesY)<=1&&(posPortico-posDesX)<=1){//TODO verificar el rango de tolerancias
							routineListener.OnMixingSucces();
						}else{
							routineListener.OnMixingFails();
						}
						break;
					}
					case(INSRT_TOOL):{
						if(posR>=59){
							routineListener.OnInsertToolSucces();
						}
						else{
							routineListener.OnInsertToolFails();
						}
						break;
					}
					case(RTRT_TOOL):{
						if(posR<=1){
							routineListener.OnRetractToolSuccess();
						}
						else{
							routineListener.OnRetractToolFails();
						}
						break;
					}
					case(RTRT_JAV):{
						
						break;
					}
					case(MOV_XY_TOOL):{
						
						break;
					}
					case(MOV_XY_JAV):{
						
						break;
					}
					case(GOTO_INSRT):{
						if((posCarro-posDesY)<=1&&(posPortico-posDesX)<=1){//TODO verificar el rango de tolerancias
							routineListener.OnGtiSucces();
						}
						else{
							routineListener.OnGtiFails();
						}
						break;
					}
					
					}
				}
			}
		} else {
			System.out.println("Fallo al actualizar");
		}
		desconectar();
	}
	

}
