
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.AdHocModuleConfiguration;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.CamBuilder;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedAcknowledgement;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedV2xMessage;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.Application;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.fed.application.app.api.VehicleApplication;
import org.eclipse.mosaic.fed.application.app.api.os.VehicleOperatingSystem;
import org.eclipse.mosaic.interactions.communication.V2xMessageTransmission;
import org.eclipse.mosaic.lib.enums.AdHocChannel;
import org.eclipse.mosaic.lib.enums.SensorType;
import org.eclipse.mosaic.lib.geo.CartesianPoint;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.vehicle.VehicleData;
import org.eclipse.mosaic.lib.util.scheduling.Event;


// ...existing imports...

import java.util.HashSet;
import java.util.Set;
import java.util.Objects;

public class CarSendsCamAPP extends AbstractApplication<VehicleOperatingSystem> implements VehicleApplication, CommunicationApplication {

    private static final int MAX_ID = 200;
    private static final long BROADCAST_INTERVAL = 5 * 1000L; // só envia a cada 5 segundos
    private long lastBroadcastTime = 0;

    // Cache para evitar processar mensagens duplicadas
    private final Set<String> processedMsgIds = new HashSet<>();

    @Override
    public void onStartup() {
        getLog().infoSimTime(this, "Initialize {} application", getOs().getId());
        getOs().getAdHocModule().enable(new AdHocModuleConfiguration()
                .addRadio()
                .channel(AdHocChannel.CCH)
                .power(16)
                .create());
    }


    @Override
    public void onVehicleUpdated(VehicleData previousVehicleData, @org.jetbrains.annotations.Nullable VehicleData updatedVehicleData) {
        long now = getOs().getSimulationTime();
        if (now - lastBroadcastTime >= BROADCAST_INTERVAL) {
            lastBroadcastTime = now;

            VehicleData data = getOs().getVehicleData();
            if (data == null) return;
            String name = data.getName();
            CartesianPoint position = getOs().getVehicleData().getPosition().toCartesian();
            double x = position.getX();
            double y = position.getY();

            final MessageRouting routing = getOperatingSystem()
                    .getAdHocModule()
                    .createMessageRouting()
                    .topoBroadCast();

            InterVehicleMsg msg = new InterVehicleMsg(routing, name, x, y);
            String msgId = name + "|" + x + "|" + y;
            if (!processedMsgIds.contains(msgId)) {
                getOs().getAdHocModule().sendV2xMessage(msg);
                processedMsgIds.add(msgId);
                double speed = data.getSpeed();
                getLog().infoSimTime(this, "Sent InterVehicleMsg: {} | {} | {} m/s", name, position, String.format("%.2f", speed));
            }
        }

        // Só faz log se detetar obstáculo
        if (getOs().getStateOfEnvironmentSensor(SensorType.OBSTACLE) > 0) {
            getLog().infoSimTime(this, "Obstacle detected by {}", getOs().getId());
        }
    }

    @Override
    public void onMessageReceived(ReceivedV2xMessage receivedV2xMessage) {
        // Só processa se necessário
        // Exemplo: evitar processar mensagens duplicadas
        if (receivedV2xMessage.getMessage() instanceof InterVehicleMsg) {
            InterVehicleMsg msg = (InterVehicleMsg) receivedV2xMessage.getMessage();
            String msgId = msg.getID() + "|" + msg.getx() + "|" + msg.gety();
            if (processedMsgIds.contains(msgId)) return;
            processedMsgIds.add(msgId);
            // ...processamento adicional se necessário...
        }
    }

    @Override
    public void processEvent(Event event) throws Exception {
        // Só faz log de eventos importantes
    }

    @Override
    public void onShutdown() {
        getLog().infoSimTime(this, "Shutdown application");
    }

    @Override
    public void onAcknowledgementReceived(ReceivedAcknowledgement acknowledgedMessage) {}
    @Override
    public void onCamBuilding(CamBuilder camBuilder) {}
    @Override
    public void onMessageTransmitted(V2xMessageTransmission v2xMessageTransmission) {}
}