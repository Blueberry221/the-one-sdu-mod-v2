package routing;

import core.*;
import java.util.*;

public class DynamicProphetRouter extends ProphetRouter {

    public static final String FORWARD_POLICY_S = "forwardPolicy";
    public static final String DROP_POLICY_S = "dropPolicy";

    private ForwardPolicy fwdPolicy;
    protected dropPolicy currentPolicy;
    private universalDropRouter.dropPolicy drpPolicy;
    private Random coinRand;

    public enum ForwardPolicy {
        RTR, GRTR_SORT, GRTR_MAX, COIN
    }

    public enum dropPolicy {
        FIFO, DO, DY, DL, DS, SHLI, LHLI, MOFO, MOPR, LEPR
    }

    public DynamicProphetRouter(Settings s) {
        super(s);
        coinRand = new Random();

        // Load Forwarding Policy
        if (s.contains(FORWARD_POLICY_S)) {
            fwdPolicy = ForwardPolicy.valueOf(s.getSetting(FORWARD_POLICY_S).toUpperCase());
        } else {
            fwdPolicy = ForwardPolicy.RTR; // Default
        }

        // Load Drop Policy
        if (s.contains(DROP_POLICY_S)) {
            currentPolicy = dropPolicy.valueOf(s.getSetting(DROP_POLICY_S).toUpperCase());
        } else {
            currentPolicy = dropPolicy.FIFO;
        }
    }

    protected DynamicProphetRouter(DynamicProphetRouter r) {
        super(r);
        this.fwdPolicy = r.fwdPolicy;
        this.currentPolicy = r.currentPolicy;
        this.coinRand = r.coinRand;
    }

    /**
     * Helper untuk probabilitas pengiriman ke tujuan.
     */
    protected double getDeliveryProbability(Message m) {
        return 0.0;
    }

    /**
     * Menghitung sisa waktu hidup pesan (Remaining TTL)
     */
    protected double getRemainingTTL(Message m) {
        return m.getTtl() - (SimClock.getTime() - m.getCreationTime());
    }

    // private Tuple<Message, Connection> tryOtherMessages() {
    // List<Tuple<Message, Connection>> messages = new ArrayList<Tuple<Message,
    // Connection>>();
    // Collection<Message> msgCollection = getMessageCollection();

    // for (Connection con : getConnections()) {
    // DTNHost other = con.getOtherNode(getHost());
    // DynamicProphetRouter othRouter = (DynamicProphetRouter) other.getRouter();

    // if (othRouter.isTransferring())
    // continue;

    // for (Message m : msgCollection) {
    // if (othRouter.hasMessage(m.getId()))
    // continue;

    // // Logika Forwarding Dasar: P(B,D) > P(A,D) atau COIN
    // boolean shouldForward = false;
    // if (fwdPolicy == ForwardPolicy.COIN) {
    // shouldForward = coinRand.nextDouble() > 0.5;
    // } else {
    // if (othRouter.getPredFor(m.getTo()) > getPredFor(m.getTo())) {
    // shouldForward = true;
    // }
    // }

    // if (shouldForward) {
    // messages.add(new Tuple<Message, Connection>(m, con));
    // }
    // }
    // }

    // if (messages.size() == 0)
    // return null;
    // Collections.sort(messages, new TupleComparator());

    // return tryMessagesForConnected(messages);
    // }

    private Tuple<Message, Connection> tryOtherMessages() {
        List<Tuple<Message, Connection>> messages = new ArrayList<Tuple<Message, Connection>>();
        Collection<Message> msgCollection = getMessageCollection();

        for (Connection con : getConnections()) {
            DTNHost other = con.getOtherNode(getHost());
            DynamicProphetRouter othRouter = (DynamicProphetRouter) other.getRouter();

            if (othRouter.isTransferring())
                continue;

            for (Message m : msgCollection) {
                if (othRouter.hasMessage(m.getId()))
                    continue;

                boolean shouldForward = false;
                if (fwdPolicy == ForwardPolicy.COIN) {
                    shouldForward = coinRand.nextDouble() > 0.5;
                } else {
                    // Logika standar: hanya kirim jika tetangga lebih baik
                    if (othRouter.getPredFor(m.getTo()) > getPredFor(m.getTo())) {
                        shouldForward = true;
                    }
                }

                if (shouldForward) {
                    messages.add(new Tuple<Message, Connection>(m, con));
                }
            }
        }

        if (messages.size() == 0)
            return null;

        // SORTING BERJALAN DI SINI
        Collections.sort(messages, new TupleComparator());

        // --- PERBAIKAN: BATASI JUMLAH PESAN ---
        // Ambil hanya 1 atau 2 pesan terbaik saja untuk dikirim per update
        // Ini memaksa router untuk memilih pesan "paling berkualitas"
        List<Tuple<Message, Connection>> limitedMessages = new ArrayList<>();
        for (int i = 0; i < Math.min(messages.size(), 2); i++) {
            limitedMessages.add(messages.get(i));
        }

        return tryMessagesForConnected(limitedMessages);
    }

    private class TupleComparator implements Comparator<Tuple<Message, Connection>> {

        public int compare(Tuple<Message, Connection> tuple1, Tuple<Message, Connection> tuple2) {
            Message m1 = tuple1.getKey();
            Message m2 = tuple2.getKey();

            DTNHost other1 = tuple1.getValue().getOtherNode(getHost());
            DTNHost other2 = tuple2.getValue().getOtherNode(getHost());

            ProphetRouter routerB1 = (ProphetRouter) other1.getRouter();
            ProphetRouter routerB2 = (ProphetRouter) other2.getRouter();

            switch (fwdPolicy) {
                case GRTR_SORT:
                    double diff1 = routerB1.getPredFor(m1.getTo()) - getPredFor(m1.getTo());
                    double diff2 = routerB2.getPredFor(m2.getTo()) - getPredFor(m2.getTo());
                    if (diff2 - diff1 == 0) {
                        return compareByQueueMode(m1, m2);
                    } else if (diff2 - diff1 < 0) {
                        return -1;
                    } else {
                        return 1;
                    }

                case GRTR_MAX:
                case RTR:
                    double pB1 = routerB1.getPredFor(m1.getTo());
                    double pB2 = routerB2.getPredFor(m2.getTo());
                    if (pB2 - pB1 == 0) {
                        return compareByQueueMode(m1, m2);
                    } else if (pB2 - pB1 < 0) {
                        return -1;
                    } else {
                        return 1;
                    }

                case COIN:
                    // Menghasilkan urutan acak setiap kali sortir dilakukan
                    return m1.getId().compareTo(m2.getId()) * (coinRand.nextBoolean() ? 1 : -1);

                // // COIN tidak punya prioritas, memakai urutan antrean asli
                // return compareByQueueMode(m1, m2);

                default:
                    return compareByQueueMode(m1, m2);
            }
        }
    }

    protected Message getMessageToDrop() {
        Collection<Message> messages = getMessageCollection();
        Message victim = null;

        for (Message m : messages) {
            if (isSending(m.getId()))
                continue;
            if (victim == null) {
                victim = m;
                continue;
            }

            switch (currentPolicy) {
                case FIFO:

                case DO:
                    // Drop Oldest: Hapus yang pertama kali masuk ke buffer node ini.
                    if (m.getReceiveTime() < victim.getReceiveTime())
                        victim = m;
                    break;

                case DY:
                    // Drop Youngest: Hapus yang paling terakhir masuk ke buffer.
                    if (m.getReceiveTime() > victim.getReceiveTime())
                        victim = m;
                    break;

                case DL:
                    // Drop Largest: Hapus pesan yang ukurannya paling besar (byte).
                    if (m.getSize() > victim.getSize())
                        victim = m;
                    break;

                case DS:
                    // Drop Smallest: Hapus pesan yang ukurannya paling kecil (byte).
                    if (m.getSize() < victim.getSize())
                        victim = m;
                    break;

                case SHLI:
                    // Shortest Remaining Life: Hapus pesan yang TTL-nya paling cepat habis.
                    if (getRemainingTTL(m) < getRemainingTTL(victim))
                        victim = m;
                    break;

                case LHLI:
                    // Longest Remaining Life: Hapus pesan yang TTL-nya paling lama habis.
                    if (getRemainingTTL(m) > getRemainingTTL(victim))
                        victim = m;
                    break;

                case MOFO:

                case MOPR:
                    // Most Frequently Forwarded: Hapus yang sudah sering "melompat" (hop count
                    // tinggi).
                    if (m.getHopCount() > victim.getHopCount())
                        victim = m;
                    break;

                case LEPR:
                    // Least Probable First: Asumsikan hop count rendah.
                    if (m.getHopCount() < victim.getHopCount())
                        victim = m;
                    break;

                default:
                    // Fallback ke FIFO jika policy tidak dikenal
                    if (m.getReceiveTime() < victim.getReceiveTime())
                        victim = m;
                    break;
            }
        }
        return victim;
    }

    @Override
    protected boolean makeRoomForMessage(int size) {
        if (size > this.getBufferSize())
            return false;

        while (getFreeBufferSize() < size) {
            Message m = getMessageToDrop();
            if (m == null)
                return false;
            deleteMessage(m.getId(), true);
        }
        return true;
    }

    @Override
    public void update() {
        super.update();
        if (!canStartTransfer() || isTransferring())
            return;
        if (exchangeDeliverableMessages() != null)
            return;

        tryOtherMessages();
    }

    @Override
    public MessageRouter replicate() {
        return new DynamicProphetRouter(this);
    }
}
