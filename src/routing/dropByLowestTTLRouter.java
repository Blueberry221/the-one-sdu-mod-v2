package routing;

import core.*;
import java.util.*;

public class dropByLowestTTLRouter extends ActiveRouter {

    public dropByLowestTTLRouter(Settings s) {
        super(s);
    }

    public dropByLowestTTLRouter(dropByLowestTTLRouter r) {
        super(r);
    }

    protected double getRemainingTTL(Message m) {
        // Rumus : initial ttl - (time now - creation time)
        return m.getTtl() - (SimClock.getTime() - m.getCreationTime());
    }

    @Override
    protected Message getOldestMessage(boolean excludeMsgBeingSent) {
        Collection<Message> messages = getMessageCollection();
        Message lowestTTLMessage = null;

        for (Message m : messages) {

            if (excludeMsgBeingSent && isSending(m.getId())) {
                continue;
            }

            if (lowestTTLMessage == null) {
                lowestTTLMessage = m;
            } else if (getRemainingTTL(lowestTTLMessage) > getRemainingTTL(m)) {
                lowestTTLMessage = m;
            }
        }
        return lowestTTLMessage;
    }

    @Override
    protected boolean makeRoomForMessage(int size) {
        if (size > this.getBufferSize()) {
            return false;
        }

        int freeBuffer = this.getFreeBufferSize();

        while (freeBuffer < size) {
            Message m = getOldestMessage(true);

            if (m == null) {
                return false;
            }

            deleteMessage(m.getId(), true);
            freeBuffer += m.getSize();
        }

        return true;
    }

    @Override
    public void update() {
        super.update();

        if (!canStartTransfer() || isTransferring()) {
            return;
        }
        if (exchangeDeliverableMessages() != null) {
            return;
        }

        tryAllMessagesToAllConnections();
    }

    @Override
    public MessageRouter replicate() {
        return new dropByLowestTTLRouter(this);
    }
}