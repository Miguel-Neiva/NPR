package src.main.java;

import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.AdHocModuleConfiguration;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.CamBuilder;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedAcknowledgement;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedV2xMessage;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.Application;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.fed.application.app.api.os.VehicleOperatingSystem;
import org.eclipse.mosaic.interactions.communication.V2xMessageTransmission;
import org.eclipse.mosaic.lib.enums.AdHocChannel;
import org.eclipse.mosaic.lib.geo.CartesianPoint;
import org.eclipse.mosaic.lib.geo.MutableCartesianPoint;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;
import org.eclipse.mosaic.lib.objects.vehicle.VehicleData;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.rti.TIME;
import org.eclipse.mosaic.lib.objects.vehicle.VehicleRoute;
import org.eclipse.mosaic.fed.application.app.api.VehicleApplication;


import java.util.*;

import static src.main.java.RSU_Program.RSU_pos;

public final class MainCarApp extends AbstractApplication<VehicleOperatingSystem> implements VehicleApplication, CommunicationApplication{

    private final static long TIME_INTERVAL = TIME.SECOND;
    private boolean ackRSU = false;

    private final HashMap<String, CartesianPoint> vizinhos = new HashMap<>();   // key -> name_carro ; value -> position_carro

    /*
    ##########################################################################################################################################3
    */

    public void putVizinho(String id, CartesianPoint pos){
        this.vizinhos.put(id, pos);
    }

    /*
    ##########################################################################################################################################3
    */

    public boolean inRangeRSU(){

        boolean res;
        CartesianPoint mypos = Objects.requireNonNull(getOs().getVehicleData()).getPosition().toCartesian();
        getLog().infoSimTime(this, "distance to RSU = {}", RSU_pos.distanceTo(mypos));

        if(RSU_pos.distanceTo(mypos) <= RSU_Program.MIN_DISTANCE_RSU){
            res = true;
        }
        else{
            res = false;
        }

        return res;
    }

    /*
    ##########################################################################################################################################3
    */

    private double distanceToRSU(CartesianPoint otherPos) {
        return otherPos.distanceTo(RSU_pos);
    }

    /*
    ##########################################################################################################################################3
    */

    // Tenta enviar mensagem à RSU em topocast
    private void sendMsgToRSU(String segredo, String rota, String id_Carro) {
        getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");

        final MessageRouting routing = getOperatingSystem()
                .getAdHocModule()
                .createMessageRouting()
                .topoCast("rsu_0", 1);

        GreenWaveMsg message_to_send = new GreenWaveMsg(routing, segredo, rota, id_Carro);
        getOs().getAdHocModule().sendV2xMessage(message_to_send);
        getLog().infoSimTime(this, "Sent to RSU = '{}'", message_to_send.toString());

        getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");

    }

    /*
    ##########################################################################################################################################3
    */

    // Tenta enviar mensagem ao vizinho que esteja mais perto da RSU
    public void sendMsgToCars(String segredo, String rota, String id_Carro){
        getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");

        if(this.vizinhos.isEmpty()){
            getLog().infoSimTime(this, "I have no neighbours to send!");
            getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
        }

        else {
            // imprimir vizinhos
            for (String car_name : this.vizinhos.keySet()) {
                getLog().infoSimTime(this, "Vizinho: {}", car_name);
            }

            double temp = 700;

            String carro_para_enviar = "";

            for (Map.Entry e : this.vizinhos.entrySet()) {
                CartesianPoint pos = (CartesianPoint) e.getValue();
                String name = (String) e.getKey();

                getLog().infoSimTime(this, "Position = {}", pos);

                double dist = distanceToRSU(pos);

                // manda para o vizinho que estiver mais perto da RSU
                if (dist <= temp) {
                    temp = dist;
                    carro_para_enviar = name;
                }
            }

            if (!carro_para_enviar.equals("")) {

                final MessageRouting routing = getOperatingSystem()
                        .getAdHocModule()
                        .createMessageRouting()
                        .topoCast(carro_para_enviar, 4);

                GreenWaveMsg message_to_send = new GreenWaveMsg(routing, segredo, rota, id_Carro);
                getOs().getAdHocModule().sendV2xMessage(message_to_send);
                getLog().infoSimTime(this, "Resent to {} - GreenWaveMsg origin in {}", carro_para_enviar, id_Carro);

                getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
            }
        }
    }

    /*
    ##########################################################################################################################################3
    */

    // Reenvia ACK de volta à origem
    public void resendACK(String final_receiver, String message){
        getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");

        if (!final_receiver.equals("")) {

            final MessageRouting routing = getOperatingSystem()
                    .getAdHocModule()
                    .createMessageRouting()
                    .topoCast(final_receiver, 4);

            RSUMsg message_to_send = new RSUMsg(routing, message, final_receiver);
            getOs().getAdHocModule().sendV2xMessage(message_to_send);
            getLog().infoSimTime(this, "Resent to {} - ACK origin in RSU", final_receiver);

            getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
        }
    }

    /*
    ##########################################################################################################################################3
    */

    @Override
    public void onMessageReceived(ReceivedV2xMessage receivedV2xMessage) {

        if (receivedV2xMessage.getMessage() instanceof RSUMsg) {

            String myID = getOs().getId();
            String receiver_id = ((RSUMsg) receivedV2xMessage.getMessage()).getId_final_receiver();
            String ACKmessage = ((RSUMsg) receivedV2xMessage.getMessage()).getMessage();
            String who_sent = receivedV2xMessage.getMessage().getRouting().getSource().getSourceName();

            if(receiver_id.equals(myID)) {
                // quando recebe da RSU
                getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
                getLog().infoSimTime(this, "Received ACK with origin at RSU from {} at {}", who_sent, getOs().getSimulationTime());
                this.ackRSU = true;
                getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
            }

            else{
                if(vizinhos.containsKey(receiver_id)){
                    resendACK(receiver_id, ACKmessage);
                }
            }
        }

        if(receivedV2xMessage.getMessage() instanceof GreenWaveMsg) {
            getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");

            String message = receivedV2xMessage.getMessage().toString();

            String id_source = receivedV2xMessage.getMessage().getRouting().getSource().getSourceName();
            getLog().infoSimTime(this, "Received GreenWaveMsg  = '{}' from {}", message, id_source);

            String segredo = ((GreenWaveMsg) receivedV2xMessage.getMessage()).getSegredo();
            String rota = ((GreenWaveMsg) receivedV2xMessage.getMessage()).getRota();
            String id = ((GreenWaveMsg) receivedV2xMessage.getMessage()).getId_carro();

            if(inRangeRSU()){
                // reenvia para a RSU se estiver dentro do seu range
                sendMsgToRSU(segredo, rota, id);
                getLog().infoSimTime(this, "Resent to RSU - GreenWaveMsg origin in {}", id);
            }

            else {
                // nao pode receber greenwave relativo a si proprio
                if (!id.equals(getOs().getId())){
                    // reencaminha a mensagem para o que estiver mais longe dele até chegar à RSU
                    sendMsgToCars(segredo, rota, id);
                }
            }
            getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
        }

        if(receivedV2xMessage.getMessage() instanceof InterVehicleMsg) {
            getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");

            String id = ((InterVehicleMsg) receivedV2xMessage.getMessage()).getID();
            double x = ((InterVehicleMsg) receivedV2xMessage.getMessage()).getx();
            double y = ((InterVehicleMsg) receivedV2xMessage.getMessage()).gety();
            getLog().infoSimTime(this, "New neighbour is {}", id);

            // adiciona o vizinho ao seu Map
            CartesianPoint posicao = new MutableCartesianPoint(x, y, 0);
            putVizinho(id, posicao);

            getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
        }
    }

    /*
    ##########################################################################################################################################3
    */

    private void sendGreenWaveMessage(){
        getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");

        // se já tem o ACK não volta a mandar GreenWave para mais ninguém
        if (this.ackRSU) {
            getLog().infoSimTime(this, "Already have ACK");
            getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
        }

        else {

            // cria a mensagem a mandar
            VehicleRoute car_route = getOs().getNavigationModule().getCurrentRoute();
            String route_id = "";
            String segredo = TrafficLightApp.SECRET;
            String id_carro = Objects.requireNonNull(getOs().getVehicleData()).getName();


            if (car_route != null) {
                route_id = car_route.getId();
                getLog().infoSimTime(this, "My Route = " + route_id);
            }

            // se a mensagem tiver conteudo envia
            if (!route_id.equals("") && !id_carro.equals("")) {

                if(inRangeRSU()){
                    sendMsgToRSU(segredo, route_id, id_carro);
                    getLog().infoSimTime(this, "Tried sending message to RSU");
                    getLog().infoSimTime(this, "ACK = {}", this.ackRSU);
                }

                else{
                    // se não estiver na range da RSU tenta mandar para o vizinho mais proximo dela
                    sendMsgToCars(segredo, route_id, id_carro);
                    getLog().infoSimTime(this, "Tried sending message to other car");
                    getLog().infoSimTime(this, "ACK = {}", this.ackRSU);
                }
            }

            getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
        }
    }

    /*
    ##########################################################################################################################################3
    */

    private void sample() {
        getOs().getEventManager().addEvent(getOs().getSimulationTime() + TIME_INTERVAL, this);
        sendGreenWaveMessage();
    }

    /*
    ##########################################################################################################################################3
    */

    @Override
    public void onVehicleUpdated(VehicleData previousVehicleData, VehicleData updatedVehicleData) {
        sample();
    }

    /*
    ##########################################################################################################################################3
    */

    @Override
    public void onStartup() {
        getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");

        getLog().infoSimTime(this, "Initialize {} application", getOs().getId());
        AdHocModuleConfiguration configuration = new AdHocModuleConfiguration()
                .addRadio()
                .channel(AdHocChannel.CCH)
                .power(16)
                .distance(20)
                .create();
        getOs().getAdHocModule().enable(configuration);
        getLog().infoSimTime(this, "Activated WLAN Module");
        getLog().infoSimTime(this, "ACK = {}", this.ackRSU);
        getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
    }

    /*
    ##########################################################################################################################################3
    */

    @Override
    public void onShutdown() {
        getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
        getLog().infoSimTime(this, "Shutdown application");
        getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
    }

    /*
    ##########################################################################################################################################3
    */

    @Override
    public void processEvent(Event event) throws Exception {
        getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
        if (!isValidStateAndLog()) {
            return;
        }
        getLog().infoSimTime(this, "Processing event");
        sample();
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
