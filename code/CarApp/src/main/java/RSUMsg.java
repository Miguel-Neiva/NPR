package src.main.java;

import org.eclipse.mosaic.lib.objects.v2x.EncodedPayload;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;

public final class RSUMsg extends V2xMessage {
    private final String  message;
    private final String  id_final_receiver;
    private final EncodedPayload payload;
    private final static long    MIN_LEN = 8L;

    public RSUMsg(MessageRouting routing, String message, String receiver) {
        super(routing);
        this.message = message;
        this.id_final_receiver = receiver;
        payload = new EncodedPayload(message.length(), MIN_LEN);
    }

    public String getMessage() {
        return message;
    }

    public String getId_final_receiver(){
        return id_final_receiver;
    }


    public EncodedPayload getPayload() {
        return null;
    }


    public EncodedPayload getPayLoad() {
        return null;
    }

    public String toString() {
        return message;
    }
}
