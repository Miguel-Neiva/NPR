import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.AdHocModuleConfiguration;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.CamBuilder;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedAcknowledgement;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedV2xMessage;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.fed.application.app.api.VehicleApplication;
import org.eclipse.mosaic.fed.application.app.api.os.VehicleOperatingSystem;
import org.eclipse.mosaic.interactions.communication.V2xMessageTransmission;
import org.eclipse.mosaic.lib.enums.AdHocChannel;
import org.eclipse.mosaic.lib.geo.CartesianPoint;
import org.eclipse.mosaic.lib.geo.MutableCartesianPoint;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.vehicle.VehicleData;
import org.eclipse.mosaic.lib.objects.vehicle.VehicleRoute;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.rti.TIME;

import java.util.*;

public final class MainCarApp extends AbstractApplication<VehicleOperatingSystem> implements VehicleApplication, CommunicationApplication {

    private MutableCartesianPoint rsuPos = null;

    public static final double MIN_DISTANCE_RSU = 100.0;

    private static final long GREENWAVE_INTERVAL = 5 * TIME.SECOND;
    private long lastGreenWaveSent = 0L;
    private final static long TIME_INTERVAL = TIME.SECOND;
    private boolean ackRSU = false;
    private final HashMap<String, CartesianPoint> vizinhos = new HashMap<>();
    private final Set<String> processedGreenWaveIds = new HashSet<>();

    public void putVizinho(String id, CartesianPoint pos) {
        CartesianPoint old = this.vizinhos.get(id);
        if (old == null || old.distanceTo(pos) > 1.0) {
            this.vizinhos.put(id, pos);
        }
    }

    public boolean inRangeRSU() {
        if (rsuPos == null) return false;
        CartesianPoint mypos = Objects.requireNonNull(getOs().getVehicleData()).getPosition().toCartesian();
        getLog().infoSimTime(this, "distance to RSU = {}", rsuPos.distanceTo(mypos));
        return mypos.distanceTo(rsuPos) <= MIN_DISTANCE_RSU;
    }

    private double distanceToRSU(CartesianPoint otherPos) {
        if (rsuPos == null) return Double.MAX_VALUE;
        return otherPos.distanceTo(rsuPos);
    }

    private void sendMsgToRSU(String segredo, String rota, String id_Carro) {
        double velocidade = Objects.requireNonNull(getOs().getVehicleData()).getSpeed();
        final MessageRouting routing = getOperatingSystem()
                .getAdHocModule()
                .createMessageRouting()
                .topoBroadCast();
        GreenWaveMsg message_to_send = new GreenWaveMsg(routing, segredo, rota, id_Carro, velocidade);
        getOs().getAdHocModule().sendV2xMessage(message_to_send);
        getLog().infoSimTime(this, "Sent to RSU = '{}'", message_to_send.toString());
    }

    public void sendMsgToCars(String segredo, String rota, String id_Carro) {
        if (this.vizinhos.isEmpty()) {
            getLog().infoSimTime(this, "I have no neighbours to send!");
            return;
        }
        double temp = Double.MAX_VALUE;
        String carro_para_enviar = "";
        for (Map.Entry<String, CartesianPoint> e : this.vizinhos.entrySet()) {
            double dist = distanceToRSU(e.getValue());
            if (dist <= temp) {
                temp = dist;
                carro_para_enviar = e.getKey();
            }
        }
        double velocidade = Objects.requireNonNull(getOs().getVehicleData()).getSpeed();
        if (!carro_para_enviar.equals("")) {
            final MessageRouting routing = getOperatingSystem()
                    .getAdHocModule()
                    .createMessageRouting()
                    .topoCast(carro_para_enviar, 4);
            GreenWaveMsg message_to_send = new GreenWaveMsg(routing, segredo, rota, id_Carro, velocidade);
            getOs().getAdHocModule().sendV2xMessage(message_to_send);
            getLog().infoSimTime(this, "Resent to {} - GreenWaveMsg origin in {}", carro_para_enviar, id_Carro);
        }
    }

    public void resendACK(String final_receiver, String message) {
        if (!final_receiver.equals("")) {
            final MessageRouting routing = getOperatingSystem()
                    .getAdHocModule()
                    .createMessageRouting()
                    .topoCast(final_receiver, 4);
            RSUMsg message_to_send = new RSUMsg(routing, message, final_receiver);
            getOs().getAdHocModule().sendV2xMessage(message_to_send);
            getLog().infoSimTime(this, "Resent to {} - ACK origin in RSU", final_receiver);
        }
    }

    @Override
    public void onMessageReceived(ReceivedV2xMessage receivedV2xMessage) {
        // Recebe posição do RSU
        if (receivedV2xMessage.getMessage() instanceof RSUMsg) {
            String msg = ((RSUMsg) receivedV2xMessage.getMessage()).getMessage();
            if (msg.startsWith("RSU_POS|")) {
                String[] parts = msg.split("\\|");
                double x = Double.parseDouble(parts[1]);
                double y = Double.parseDouble(parts[2]);
                rsuPos = new MutableCartesianPoint(x, y, 0);
                getLog().infoSimTime(this, "RSU position received: {}, {}", x, y);
                return;
            }

            String myID = getOs().getId();
            String receiver_id = ((RSUMsg) receivedV2xMessage.getMessage()).getId_final_receiver();
            String ACKmessage = ((RSUMsg) receivedV2xMessage.getMessage()).getMessage();
            String who_sent = receivedV2xMessage.getMessage().getRouting().getSource().getSourceName();
            if (receiver_id.equals(myID)) {
                getLog().infoSimTime(this, "Received ACK with origin at RSU from {} at {}", who_sent, getOs().getSimulationTime());
                this.ackRSU = true;
            } else {
                if (vizinhos.containsKey(receiver_id)) {
                    resendACK(receiver_id, ACKmessage);
                }
            }
        }

        if (receivedV2xMessage.getMessage() instanceof GreenWaveMsg) {
            String segredo = ((GreenWaveMsg) receivedV2xMessage.getMessage()).getSegredo();
            String rota = ((GreenWaveMsg) receivedV2xMessage.getMessage()).getRota();
            String id = ((GreenWaveMsg) receivedV2xMessage.getMessage()).getId_carro();
            String msgId = segredo + "|" + rota + "|" + id;

            // *** FILTRO: só processa mensagens da minha rota ***
            VehicleRoute myRoute = getOs().getNavigationModule().getCurrentRoute();
            if (myRoute == null || !rota.equals(myRoute.getId())) {
                return; // Ignora mensagens de outras rotas
            }

            if (processedGreenWaveIds.contains(msgId)) return;
            processedGreenWaveIds.add(msgId);

            if (inRangeRSU()) {
                sendMsgToRSU(segredo, rota, id);
                getLog().infoSimTime(this, "Resent to RSU - GreenWaveMsg origin in {}", id);
            } else {
                if (!id.equals(getOs().getId())) {
                    sendMsgToCars(segredo, rota, id);
                }
            }
              if (receivedV2xMessage.getMessage() instanceof FogMetricsMsg) {
        FogMetricsMsg metrics = (FogMetricsMsg) receivedV2xMessage.getMessage();
        getLog().infoSimTime(this, "Received metrics from FogNode: avgR0={}, avgR1={}", metrics.getAvgR0(), metrics.getAvgR1());
    }

        }

        if (receivedV2xMessage.getMessage() instanceof InterVehicleMsg) {
            String id = ((InterVehicleMsg) receivedV2xMessage.getMessage()).getID();
            double x = ((InterVehicleMsg) receivedV2xMessage.getMessage()).getx();
            double y = ((InterVehicleMsg) receivedV2xMessage.getMessage()).gety();
            CartesianPoint posicao = new MutableCartesianPoint(x, y, 0);
            putVizinho(id, posicao);
        }
    }

    private void sendGreenWaveMessage() {
        if (this.ackRSU) {
            getLog().infoSimTime(this, "Already have ACK");
            return;
        }
        VehicleRoute car_route = getOs().getNavigationModule().getCurrentRoute();
        String route_id = "";
        String segredo = TrafficLightApp.SECRET;
        String id_carro = Objects.requireNonNull(getOs().getVehicleData()).getName();
        if (car_route != null) {
            route_id = car_route.getId();
        }
        if (!route_id.equals("") && !id_carro.equals("")) {
            if (inRangeRSU()) {
                sendMsgToRSU(segredo, route_id, id_carro);
            } else {
                sendMsgToCars(segredo, route_id, id_carro);
            }
        }
    }

    private void sample() {
        getOs().getEventManager().addEvent(getOs().getSimulationTime() + TIME_INTERVAL, this);
        sendGreenWaveMessage();
    }

    @Override
    public void onVehicleUpdated(VehicleData previous, VehicleData updated) {
        long now = getOs().getSimulationTime();

        // Envia GreenWaveMsg periodicamente, mesmo parado
        if (now - lastGreenWaveSent >= GREENWAVE_INTERVAL) {
            lastGreenWaveSent = now;

            VehicleData data = getOs().getVehicleData();
            if (data == null) return;
            String id_carro = data.getName();
            String segredo = TrafficLightApp.SECRET;
            VehicleRoute car_route = getOs().getNavigationModule().getCurrentRoute();
            String route_id = car_route != null ? car_route.getId() : "";

            if (!route_id.isEmpty() && !id_carro.isEmpty()) {
                MessageRouting routing = getOperatingSystem()
                        .getAdHocModule()
                        .createMessageRouting()
                        .topoBroadCast();
                double velocidade = data.getSpeed();
                GreenWaveMsg msg = new GreenWaveMsg(routing, segredo, route_id, id_carro, velocidade);
                getOs().getAdHocModule().sendV2xMessage(msg);
                getLog().infoSimTime(this, "Sent periodic GreenWaveMsg: {} on route {}", id_carro, route_id);
            }
        }

        sample();
    }

    @Override
    public void onStartup() {
        getLog().infoSimTime(this, "Initialize {} application", getOs().getId());
        AdHocModuleConfiguration configuration = new AdHocModuleConfiguration()
                .addRadio()
                .channel(AdHocChannel.CCH)
                .power(16)
                .distance(20)
                .create();
        getOs().getAdHocModule().enable(configuration);
    }

    @Override
    public void onShutdown() {
        getLog().infoSimTime(this, "Shutdown application");
    }

    @Override
    public void processEvent(Event event) throws Exception {
        sample();
    }

    @Override
    public void onAcknowledgementReceived(ReceivedAcknowledgement receivedAcknowledgement) {}
    @Override
    public void onCamBuilding(CamBuilder camBuilder) {}
    @Override
    public void onMessageTransmitted(V2xMessageTransmission v2xMessageTransmission) {}
}