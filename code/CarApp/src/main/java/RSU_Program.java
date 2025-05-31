package src.main.java;

import org.apache.commons.lang3.Validate;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.AdHocModuleConfiguration;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.CamBuilder;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedAcknowledgement;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedV2xMessage;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.fed.application.app.api.os.RoadSideUnitOperatingSystem;
import org.eclipse.mosaic.interactions.communication.V2xMessageTransmission;
import org.eclipse.mosaic.lib.enums.AdHocChannel;
import org.eclipse.mosaic.lib.geo.CartesianPoint;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.util.scheduling.DefaultEventScheduler;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.lib.util.scheduling.EventProcessor;
import org.eclipse.mosaic.rti.TIME;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static src.main.java.TrafficLightApp.*;

public class RSU_Program extends AbstractApplication<RoadSideUnitOperatingSystem> implements CommunicationApplication {

   private static final long TIME_INTERVAL = 2000000000L;
   private final HashMap<String, Set<String>> carros = new HashMap<>();   // key -> id_route ; value -> set de ids de carros
   private final Set<String> temp = new HashSet<>();   // value -> set de ids de todos os carros que já passaram
   static final Integer MIN_DISTANCE_RSU = 20;

   private final int CHECK_COUNTERS_INTERVAL = 2;

   static CartesianPoint RSU_pos;

   // usaddo para ver quantos veículos foram identificados pelo RSU
   private final HashMap<String, Set<String>> total_carros = new HashMap<>();  // key -> id_route ; value -> set de ids de carros

   // usado para ver quantas vezes recebeu o RSU o mesmo pacote
   private final HashMap<String, Integer> carro_pacotes = new HashMap<>();  // key -> id_route ; value -> quantos greenwaves mandou

   /*
    ##########################################################################################################################################3
    */

   private void RSU_position() {
      RSU_pos = getOs().getPosition().toCartesian();
   }

   /*
    ##########################################################################################################################################3
    */

   // nao é usado mas pode ser util
   private void sendAdHocBroadcast() {
      MessageRouting routing = ((RoadSideUnitOperatingSystem)this.getOs()).getAdHocModule().createMessageRouting().viaChannel(AdHocChannel.CCH).topoBroadCast();
      RSUMsg message = new RSUMsg(routing, ((RoadSideUnitOperatingSystem)this.getOs()).getPosition().toString(), "");
      ((RoadSideUnitOperatingSystem)this.getOs()).getAdHocModule().sendV2xMessage(message);
   }

   /*
    ##########################################################################################################################################3
    */

   public void checkCounters() {
      if (!this.carros.isEmpty()) {

         int counter_r0 = this.carros.get("r_0").size();
         int counter_r1 = this.carros.get("r_1").size();

         getLog().infoSimTime(this, "Counter R0 = {} , Counter R1 = {}", counter_r0, counter_r1);

         if (1 <= counter_r0 && counter_r1 <= counter_r0) {
            sendTopoCastMessage_trafic_ligth("tl_0", 1, "0");
            this.carros.get("r_0").clear();
            getLog().infoSimTime(this, "Cleared: Counter R0 = {} , Counter R1 = {}", this.carros.get("r_0").size(), counter_r1);
         } else if (1 <= counter_r1) {
            sendTopoCastMessage_trafic_ligth("tl_0", 1, "2");
            this.carros.get("r_1").clear();
            getLog().infoSimTime(this, "Cleared: Counter R0 = {} , Counter R1 = {}", counter_r0, this.carros.get("r_1").size());
         }
      }
   }

   /*
    ##########################################################################################################################################3
    */

   public void sample() {
      ((RoadSideUnitOperatingSystem)this.getOs()).getEventManager().addEvent(((RoadSideUnitOperatingSystem)this.getOs()).getSimulationTime() + TIME_INTERVAL, new EventProcessor[]{this});
   }

   /*
    ##########################################################################################################################################3
    */

   public void onStartup() {
      getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");

      this.getLog().infoSimTime(this, "Initialize application", new Object[0]);
      ((RoadSideUnitOperatingSystem)this.getOs()).getAdHocModule().enable((new AdHocModuleConfiguration()).addRadio().channel(AdHocChannel.CCH).power(50.0D).create());
      this.getLog().infoSimTime(this, "Activated WLAN Module", new Object[0]);
      this.sample();

      Set<String> r0_cars = new HashSet<>();
      Set<String> r1_cars = new HashSet<>();
      this.carros.put("r_0", r0_cars);
      this.carros.put("r_1", r1_cars);

      Set<String> r0_total_cars = new HashSet<>();
      Set<String> r1_total_cars = new HashSet<>();
      this.total_carros.put("r_0", r0_total_cars);
      this.total_carros.put("r_1", r1_total_cars);

      RSU_position();   // ir buscar a posição da RSU

      // Apenas verifica os counters a cada 2 segundos da vida real!
      // Serve apenas para permitir que acumule mais transito, melhorando assim a componente visual da simulação.
      ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
      Runnable task = new Runnable() {
         @Override
         public void run() {
            checkCounters();
         }
      };
      scheduler.scheduleAtFixedRate(task, 0, CHECK_COUNTERS_INTERVAL, TimeUnit.SECONDS);

      getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
   }

   /*
    ##########################################################################################################################################3
    */

   public int getCarsR0() {
      if(!total_carros.isEmpty()){
         return total_carros.get("r_0").size();
      }
      else{
         return 0;
      }
   }

   /*
    ##########################################################################################################################################3
    */

   public int getCarsR1() {
      if(!total_carros.isEmpty()){
         return total_carros.get("r_1").size();
      }
      else{
         return 0;
      }
   }

   /*
    ##########################################################################################################################################3
    */

   public String getTotalMessages() {
      if(!carro_pacotes.isEmpty()){
         String res = "\n";

         res += "+--------+--------------------+\n";
         res += "|  CAR   | Nº MESSAGES TO RSU |\n";
         res += "+--------+--------------------+\n";

         for(Map.Entry e : carro_pacotes.entrySet()){
            String id = (String) e.getKey();
            int i = (int) e.getValue();
            res += "|   " + id + "   ->   " + i + "   |\n";
         }

         return res;

      }
      else{
         return "NO MESSAGES COLLECTED";
      }
   }

   /*
    ##########################################################################################################################################3
    */


   public void onShutdown() {
      getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");

      getLog().infoSimTime(this, "Cars in route r0 (vertical) = {}", getCarsR0());
      getLog().infoSimTime(this, "Cars in route r1 (horizontal) = {}", getCarsR1());

      getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");

      getLog().infoSimTime(this, "Total GreenWave messages sent:");
      getLog().infoSimTime(this, "{}", getTotalMessages());

      getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
      this.getLog().infoSimTime(this, "Shutdown application", new Object[0]);
      getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
   }

   /*
    ##########################################################################################################################################3
    */

   public void processEvent(Event event) throws Exception {
      this.sample();
   }

   /*
    ##########################################################################################################################################3
    */

   // nao é usado mas pode ser util
   private void sendTopoBroadcastMessage(String message) {
      getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");

      final MessageRouting routing = getOperatingSystem()
              .getAdHocModule()
              .createMessageRouting()
              .topoBroadCast();


      getOs().getAdHocModule().sendV2xMessage(new RSUMsg(routing, message, ""));
      getLog().infoSimTime(this, "Sent message for Traffic Ligth");
      getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
   }

   /*
    ##########################################################################################################################################3
    */

   // Envio de RSU msg com log especifico para o semaforo
   private void sendTopoCastMessage_trafic_ligth(String receiver, int hops, String message) {
      getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");

      final MessageRouting routing = getOperatingSystem()
              .getAdHocModule()
              .createMessageRouting()
              .topoCast(receiver, hops);


      getOs().getAdHocModule().sendV2xMessage(new RSUMsg(routing, message, ""));
      getLog().infoSimTime(this, "Sent message to {} - Change to program {}", receiver, message);

      getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
   }

   /*
    ##########################################################################################################################################3
    */

   // Envio de RSU msg com log especifico para os carros
   private void sendTopoCastMessage_ACK(String final_receiver, String who_sent, int hops, String message) {
      getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");

      final MessageRouting routing = getOperatingSystem()
              .getAdHocModule()
              .createMessageRouting()
              .topoCast(who_sent, hops);

      getOs().getAdHocModule().sendV2xMessage(new RSUMsg(routing, message, final_receiver));
      getLog().infoSimTime(this, "Sent ACK message to {} with final destiny to {}", who_sent, final_receiver);

      getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
   }

   /*
    ##########################################################################################################################################3
    */

   @Override
   public void onMessageReceived(ReceivedV2xMessage receivedV2xMessage) {
      getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");

      // validar se a posição do carro está dentro do range da RSU
      Validate.notNull(receivedV2xMessage.getMessage().getRouting().getSource().getSourcePosition(),
              "The source position of the sender cannot be null");
      if (!(receivedV2xMessage.getMessage().getRouting().getSource().getSourcePosition()
              .distanceTo(getOs().getPosition()) <= MIN_DISTANCE_RSU)) {
         getLog().infoSimTime(this, "Vehicle that sent message is too far away.");
      }

      else {

         String secret = "";
         String route = "";
         String id_carro = "";

         if (!(receivedV2xMessage.getMessage() instanceof GreenWaveMsg)) {
            return;
         }

         String who_sent = receivedV2xMessage.getMessage().getRouting().getSource().getSourceName();


         String message = receivedV2xMessage.getMessage().toString();
         getLog().infoSimTime(this, "Received GreenWaveMsg = '{}' from {}", message, who_sent);

         secret = ((GreenWaveMsg) receivedV2xMessage.getMessage()).getSegredo();
         route = ((GreenWaveMsg) receivedV2xMessage.getMessage()).getRota();
         id_carro = ((GreenWaveMsg) receivedV2xMessage.getMessage()).getId_carro();

         if (!(secret.equals(SECRET))) {
            return;
         }

         getLog().infoSimTime(this, "Received correct passphrase - {} - from {}", SECRET, id_carro);

         if(total_carros.containsKey(id_carro)){
            getLog().infoSimTime(this, "Vou incrementar");
            int i = carro_pacotes.get(id_carro) + 1;
            carro_pacotes.put(id_carro, i);
         }

         // adiciona o id do carro ao set da route
         if (!(temp.contains(id_carro))) {
            temp.add(id_carro);
            carros.get(route).add(id_carro);
            total_carros.get(route).add(id_carro);
            carro_pacotes.put(id_carro, 1);
         }

         getLog().infoSimTime(this, "Route {} = {}", route, carros.get(route));

         // envia o ACK de volta
         getLog().infoSimTime(this, "Sending ACK to {}", id_carro);
         sendTopoCastMessage_ACK(id_carro, who_sent, 4, "ACK");
      }

      getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
   }

   /*
    ##########################################################################################################################################3
    */

   @Override
   public void onAcknowledgementReceived(ReceivedAcknowledgement receivedAcknowledgement) {

   }

   @Override
   public void onCamBuilding(CamBuilder camBuilder) {

   }

   @Override
   public void onMessageTransmitted(V2xMessageTransmission v2xMessageTransmission) {

   }

   /*
    ##########################################################################################################################################3
    */

}
