/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package supervisor.maquina;




import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 *
 * @author Francisco
 */
public class ControlMaquinaController implements Initializable {
	// TODO medir y llenarGruaCnc grua=new GruaCnc(x_0, y_0, z_0, l_a, r_a, d_e,
		// r_x, r_y, r_z, r_r, r_p, r_v, x_max, x_j, y_j, x_min, y_max, y_min,
		// z_max, z_min, dr, x_v1, y_v1, ar, ai, lr, lv_r, derX, derY)
		// TODO corregir las distancias de la javalina
	
	Double x0=214.1;
	Double y0=86.3;
	Double z0=211.0;
	Double la=192.0;
	Double ra=11.22;
	Double de=15.0;	
	Double relx = 30.0 / 17.8;
	Double rely = 40.0 / 17.8;
	Double relz = 40.0 / 17.8;
	Double relr = 40.0 / 360 * 2.4;
	Double relp = 2500.0;
	Double relv = 41.66667;//relp/60.0;
	


	Double xmax = 300.0;
	Double xmin = 0.0;

	Double ymax = 900.0;
	Double ymin = 0.0;

	Double zmax = 0.0;
	Double zmin = -52.0;

	Double rmax = 109.0;
	Double rmin = 0.0;

	Double dis_res = ra + 10; // distancia restringida en el sistema

	Double xv1 = 329.0;
	Double yv1 = 100.0;

	Double ar = 400.0;
	Double ai = 10.0;
	Double lr = 203.0;

	Double derX = 90.0;
	Double derY = 69.0;
	
	EquipoCompost grua = new EquipoCompost(x0, y0, z0, la, ra, de, relx,
			rely, relz, relr, relp, relv, xmax, 0.0, 0.0, xmin, ymax, ymin,
			zmax, zmin, dis_res, xv1, yv1, ar, ai, lr, lr, derX, derY,
			"20.0.0.88", (short) 502);
	
	
	// variables usadas para generacion de rutina
	Boolean rutina = false;
	int paso_rut = 0;
	Boolean rutina_tablet = false;
	int paso_rut_tablet = 0;
	String pathSp;
	//

	Double temperaturas[] = new Double[4];
	int nMuestras = 20;
	// variables para muestra de temperaturas
	CategoryAxis ejeX = new CategoryAxis();
	NumberAxis ejeY = new NumberAxis("Temperatura", 0, 40, 1);

	// manejo de datos
	@FXML
	BarChart<String, Number> bcjavalinas = new BarChart<>(ejeX, ejeY);
	// coso sebas

	// variables de control
	Boolean ctablet = false;
		
	// //////////////////


	@FXML
	private Label lblConeccion;

	// labels de muestra de datos de posicion
	// posiciones del cnc
	@FXML
	private Label PX;
	@FXML
	private Label PY;
	@FXML
	private Label PZ;
	@FXML
	private Label PR;
	// posiciones de la punta
	@FXML
	private Label PosX;
	@FXML
	private Label PosY;
	@FXML
	private Label PosZ;
	@FXML
	private Label PosR;
	@FXML
	private Label PosRY;
	@FXML
	private Label PosRX;
	@FXML
	private Label rAct;
	@FXML
	private Label sAct;

	// //cuadros de texto para movimiento
	@FXML
	private TextField txtFldPosX;
	@FXML
	private TextField txtFldPosY;
	@FXML
	private TextField txtFldPosZ;
	@FXML
	private TextField txtFldPosR;
	// MOVIEMIENTO EN REACTOR
	@FXML
	private TextField txtFldPosXrel;
	@FXML
	private TextField txtFldPosYrel;
	@FXML
	private TextField txtFldPosZrel;
	@FXML
	private TextField txtFldAng;
	@FXML
	private TextField txtFldReactor;
	@FXML
	private TextField txtFldSector;

	// cuadros de texto para configuraciOn

	// configuración eje x
	@FXML
	private TextField txtFldViX;
	@FXML
	private TextField txtFldVmX;
	@FXML
	private TextField txtFldAccX;
	@FXML
	private TextField txtFldPdX;
	// configurtacion eje y
	@FXML
	private TextField txtFldViY;
	@FXML
	private TextField txtFldVmY;
	@FXML
	private TextField txtFldAccY;
	@FXML
	private TextField txtFldPdY;
	// configuracion eje z
	@FXML
	private TextField txtFldViZ;
	@FXML
	private TextField txtFldVmZ;
	@FXML
	private TextField txtFldAccZ;
	@FXML
	private TextField txtFldPdZ;
	// configuracion eje R
	@FXML
	private TextField txtFldViR;
	@FXML
	private TextField txtFldVmR;
	@FXML
	private TextField txtFldAccR;
	@FXML
	private TextField txtFldPdR;

	// Comando del variador de frecuencia
	@FXML
	private TextField txtFldVelVar;
	@FXML
	private TextField txtFldSentVar;

	@FXML
	private void handleMoverAgitador(ActionEvent e) {
		grua.toolOn();
	}

	@FXML
	private void handleDetenerAgitador(ActionEvent e) {
		grua.toolOff();
	}

	@FXML
	// coneccion con cnc
	private TextField txtFldPuerto;
	@FXML
	private TextField txtFldIP;

	@FXML
	private ChoiceBox<Short> chBoxEjes;

	// botones
	@FXML
	private Button btnConfigurar;

	// eventos para temperatura
	Boolean adq = false;

	@FXML
	private void handleIniciarTemp(ActionEvent e) {

	}

	@FXML
	private void handleDetenerTemp(ActionEvent e) {

	}

	// EVENTOS DE COMANDO DE MOVIMIENTO
	@FXML
	private void handleButtonMoverLibre(ActionEvent e) {
		grua.free_move(Double.parseDouble(txtFldPosX.getText()),
				Double.parseDouble(txtFldPosY.getText()),
				Double.parseDouble(txtFldPosZ.getText()),
				Double.parseDouble(txtFldPosR.getText()));
	}

	@FXML
	private void handleButtonMoverHerramienta(ActionEvent e) {
		grua.movXY_tool(Double.parseDouble(txtFldPosX.getText()),
				Double.parseDouble(txtFldPosY.getText()));
	}

	@FXML
	private void handleButtonIrReactor(ActionEvent e) {
		 grua.gotoInsrtPoint(Integer.parseInt(txtFldReactor.getText()),
		 Integer.parseInt(txtFldSector.getText()),
		 Double.parseDouble(txtFldPosXrel.getText()),
		 Double.parseDouble(txtFldPosYrel.getText()));
//		rutina = true;
//		paso_rut =0;
//		grua.movListener.OnMovementSucces(1);
		//grua.movListener.OnDeviceIsBussy();
	}

	@FXML
	private void handleButtonIntroHerramienta(ActionEvent e) {
		grua.insrtTool();
	}

	@FXML
	private void handleButtonRetHerramienta(ActionEvent e) {
		grua.rtrtTool();
	}

	@FXML
	private void handleButtonMezclar(ActionEvent e) {
		grua.mixingRoutine(31, 1, "C://Users//Francisco//Desktop//demo-A.txt");//TODO verificar tema rutinas
		rutina = true;
		paso_rut = 0;
	}

	// eventos de botones de configuracion


	Double coso = 0.000;

	// eventos de botones de configruacion
	@FXML
	private void handleButtonConnect(ActionEvent e) {
		grua.conectar_asinc();
	}

	@FXML
	private void handleButtonConfigAll(ActionEvent e) {
		//TODO organizar configuracion general
		// controlador.mov_parameter(axis, init_speed, speed, acc, pmm, num);
	}

	@FXML
	private void handleButtonConfigX(ActionEvent e) {
		grua.configX(Double.parseDouble(txtFldViX.getText()),
				Double.parseDouble(txtFldVmX.getText()),
				Double.parseDouble(txtFldAccX.getText()));
	}

	@FXML
	private void handleButtonConfigY(ActionEvent e) {
		grua.configY(Double.parseDouble(txtFldViY.getText()),
				Double.parseDouble(txtFldVmY.getText()),
				Double.parseDouble(txtFldAccY.getText()));
	}

	@FXML
	private void handleButtonConfigZ(ActionEvent e) {
		grua.configZ(Double.parseDouble(txtFldViZ.getText()),
				Double.parseDouble(txtFldVmZ.getText()),
				Double.parseDouble(txtFldAccZ.getText()));
	}

	@FXML
	private void handleButtonConfigR(ActionEvent e) {
		grua.configR(Double.parseDouble(txtFldViR.getText()),
				Double.parseDouble(txtFldVmR.getText()),
				Double.parseDouble(txtFldAccR.getText()));
	}

	// EVENTOS DE BOTONES DE COMANDOS BASICOS
	@FXML
	void handleButtonDetener(ActionEvent e) {
		ctablet = false;
		rutina = false;
		paso_rut = 0;
		grua.routine=false;
		grua.detener();
		grua.toolOff();

	}

	@Override
	public void initialize(URL url, ResourceBundle rb) {

		grua.actualizador.play();

		PX.textProperty().bind(grua.show_posPortico);
		PY.textProperty().bind(grua.show_posCarro);
		PZ.textProperty().bind(grua.show_posElevador);
		PR.textProperty().bind(grua.show_posRotador);

		PosRX.textProperty().bind(grua.show_xRel);
		PosRY.textProperty().bind(grua.show_yRel);

		rAct.textProperty().bind(grua.show_rActual);
		sAct.textProperty().bind(grua.show_sActual);

		PosX.textProperty().bind(grua.show_posX);
		PosY.textProperty().bind(grua.show_posY);
		PosZ.textProperty().bind(grua.show_posZ);
		PosR.textProperty().bind(grua.show_posR);

		txtFldAccX.setText("900");
		txtFldViX.setText("0");
		txtFldVmX.setText("1500");

		txtFldAccY.setText("900");
		txtFldViY.setText("0");
		txtFldVmY.setText("1700");

		txtFldAccZ.setText("600");
		txtFldViZ.setText("0");
		txtFldVmZ.setText("1200");

		txtFldAccR.setText("50");
		txtFldViR.setText("0");
		txtFldVmR.setText("100");

		txtFldIP.setText("20.0.0.88");
		txtFldPuerto.setText("502");

		txtFldPosX.setText("0");
		txtFldPosY.setText("0");
		txtFldPosZ.setText("0");
		txtFldPosR.setText("0");
		
		grua.setOnMixingRoutineListener(new EquipoCompost.OnMixingRoutineListener() {
			
			@Override
			public void OnMixingSucces() {
				// TODO Auto-generated method stub
				if(rutina){
					if(paso_rut==0){
						grua.mixingRoutine(31, 2, "C://Users//Francisco//Desktop//demo-B.txt");
					}
					else{
						rutina=false;
						grua.toolOff();
						grua.free_move(0.0, 0.0, 0.0, 0.0);
					}
					paso_rut++;
				}
				if(rutina_tablet){
					if(paso_rut_tablet==0){
						grua.mixingRoutine(0, 2, pathSp);
					}
					else{
						rutina_tablet=false;
						grua.toolOff();
						grua.free_move(0.0, 0.0, 0.0, 0.0);
					}
					paso_rut_tablet++;
				}
			}
			
			@Override
			public void OnMixingStarts() {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void OnMixingFails() {
				// TODO Auto-generated method stub
				grua.detener();
				rutina=false;
				if(rutina_tablet){
					grua.free_move(0.0, 0.0, 0.0, 0.0);
				}
				rutina_tablet=false;
				
				
				grua.routine=false;
				grua.toolOff();
			}
		});
		

		grua.setOnConnectionListener(new GruaCnc.OnConnectionListener() {

			@Override
			public void OnConnectionSucces() {
				lblConeccion.setText("Conectado");
			}

			@Override
			public void OnConnectionExistsErr() {
				throw new UnsupportedOperationException("Not supported yet.");
			}

			@Override
			public void OnConnectionMaxLimitErr() {
				throw new UnsupportedOperationException("Not supported yet.");
			}

			@Override
			public void OnConnectionSocketErr() {
				throw new UnsupportedOperationException("Not supported yet.");
			}

			@Override
			public void OnConnectionTcpErr() {
				throw new UnsupportedOperationException("Not supported yet.");
			}
		});

		grua.setOnOnMovementListener(new GruaCnc.OnMovementListener() {

			@Override
			public void OnPositionUpdate(Double x, Double y, Double z, Double r) {
				// TODO Auto-generated method stub

			}

			@Override
			public void OnOutOfBounds() {
				// TODO Auto-generated method stub

			}

			@Override
			public void OnMovementSucces(int mov) {
				// TODO Auto-generated method stub
				System.out.println("Termine de moverme!!! " + mov);


			}

			@Override
			public void OnDeviceIsBussy() {
				// TODO Auto-generated method stub
			}

			@Override
			public void OnCommandFailed(String fuente) {
				// TODO Auto-generated method stub

			}
		});

	}

}
