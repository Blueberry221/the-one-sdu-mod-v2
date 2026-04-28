package routing;

import core.Message;
import core.Settings;
import java.util.*;

public class SHLIProphetRouter extends ProphetRouter {

    public SHLIProphetRouter(Settings s) {
        super(s);
    }

    protected SHLIProphetRouter(SHLIProphetRouter r) {
        super(r);
    }

    @Override
    protected void makeRoomForNewMessage(int s) {
        List<Message> messageBuffer = new ArrayList<>(getMessageCollection());
        if (messageBuffer.size() == 0)

            Collections.sort(messageBuffer, new Comparator<Message>() {
                @Override
                public int compare(Message m1, Message m2) {
                    // Sisa waktu = TTL - (Waktu Sekarang - Waktu Pembuatan)
                    double m1Remaining = m1.getTtl() - (core.SimClock.getTime() - m1.getCreationTime());
                    double m2Remaining = m2.getTtl() - (core.SimClock.getTime() - m2.getCreationTime());

                    return Double.compare(m1Remaining, m2Remaining);
                }
            });

        for (Message m : messageBuffer) {
            if (getFreeBufferSize() >= s) {
                break; // Ruang sudah cukup
            }
            deleteMessage(m.getId(), true);
        }
    }

    @Override
    public SHLIProphetRouter replicate() {
        return new SHLIProphetRouter(this);
    }
}
