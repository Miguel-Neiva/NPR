package src.main.java;

import org.eclipse.mosaic.lib.objects.v2x.EncodedPayload;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;

public final class InterVehicleMsg extends V2xMessage {
    private final EncodedPayload payload = new EncodedPayload(16L, 128L);
    private static final long minLen = 128L;
    private String id;
    private double pos_x;
    private double pos_y;

    public InterVehicleMsg(MessageRouting routing, String id, double x, double y) {
        super(routing);
        this.id = id;
        this.pos_x = x;
        this.pos_y = y;
    }

    public String getMessage() {
        return id + " | " + pos_x + " | " + " | " + pos_y;
    }

    public String getID() {
        return this.id;
    }

    public double getx() {
        return this.pos_x;
    }

    public double gety() {
        return this.pos_y;
    }


    public EncodedPayload getPayload() {
        return null;
    }

    public EncodedPayload getPayLoad() {
        return null;
    }

    public String toString() {
        return this.getMessage();
    }
}