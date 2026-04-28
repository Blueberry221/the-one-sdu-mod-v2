package routing.community;

import java.util.*;
import core.*;
import routing.*;

public class PeopleRankEngine implements RoutingDecisionEngine {
    public static final String PEOPLERANK_NS = "PeopleRankEngine";
    public static final String MSG_PEOPLERANK_PROP = "PeopleRank";

    private double myRank;
    private double dampingFactor;
    private double rankThreshold;
    private int minContactToFriend;

    private Set<DTNHost> friends;
    private Map<DTNHost, Double> contributions;
    private Map<DTNHost, Integer> contactCounts;

    /**
     * Konstruktor utama
     */
    public PeopleRankEngine(Settings s) {
        // Membaca nilai dari file .txt, jika tidak memakai default 0.1
        this.myRank = s.getDouble("initialRank", 0.1);

        // Membaca faktor redaman dari file .txt, jika tidak memakai default 0.85
        this.dampingFactor = s.getDouble("dampingFactor", 0.85);

        // Membaca threshold untuk forward pesan, jika tidak memakai default 1.2
        this.rankThreshold = s.getDouble("rankThreshold", 1.2);

        // Membaca jumlah minimal kontak untuk dianggap teman, jika tidak memakai default 5
        this.minContactToFriend = s.getInt("minContactToFriend", 5);

        this.friends = new HashSet<>();
        this.contributions = new HashMap<>();
        this.contactCounts = new HashMap<>();
    }

    /**
     * Copy Constructor
     */
    private PeopleRankEngine(PeopleRankEngine prototype) {
        this.myRank = prototype.myRank;
        this.dampingFactor = prototype.dampingFactor;
        this.rankThreshold = prototype.rankThreshold;
        this.minContactToFriend = prototype.minContactToFriend;

        this.friends = new HashSet<>();
        this.contributions = new HashMap<>();
        this.contactCounts = new HashMap<>();
    }

    // while i is in contact with j d
    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        PeopleRankEngine peerEngine = getOtherEngine(peer);

        // if j ∈ F(i) then
        if (peerEngine != null) {
            int count = contactCounts.getOrDefault(peer, 0) + 1;
            contactCounts.put(peer, count);

            // Menhitung rank hanya jika sudah bertemu minimal 5 kali
            // 5 kali bertemu, otomatis masuk ke dalam set teman (F(i))
            if (count >= minContactToFriend) {
                this.friends.add(peer);

                // receive(PeR(j), |F(j)|)
                double peerRank = peerEngine.getRank();
                int peerFriendsCount = peerEngine.getFriendsCount();
                double contribution = (peerFriendsCount > 0) ? (peerRank / peerFriendsCount) : 0;

                contributions.put(peer, contribution);

                // Menghitung total kontribusi dari semua teman
                double contributionSum = 0;
                for (double val : contributions.values()) {
                    contributionSum += val;
                }

                // update(PeR(i))
                this.myRank = (1 - dampingFactor) + (dampingFactor * contributionSum);
            }
        }
    }
    // @Override
    // public void doExchangeForNewConnection(Connection con, DTNHost peer) {
    // MessageRouter peerRouter = peer.getRouter();

    // // Memastikan tetangga menggunakan DecisionEngineRouter
    // if (peerRouter instanceof DecisionEngineRouter) {
    // DecisionEngineRouter deRouter = (DecisionEngineRouter) peerRouter;

    // // Ambil engine spesifik PeopleRank dari dalam router tetangga
    // RoutingDecisionEngine engine = deRouter.getDecisionEngine();

    // if (engine instanceof PeopleRankEngine) {
    // PeopleRankEngine peerEngine = (PeopleRankEngine) engine;

    // /*
    // * * RUMUS PEOPLERANK:
    // * Rank baru = (1 - d) + d * (Rank Tetangga)
    // */
    // this.myRank = (1 - dampingFactor) + dampingFactor * (peerEngine.getRank());
    // }
    // }
    // }

    // while ∃ m ∈ buffer(i) do
    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
        // if j = destination(m)
        if (m.getTo().equals(otherHost)) {
            return true;
        }

        PeopleRankEngine otherEngine = getOtherEngine(otherHost);

        if (otherEngine != null) {
            /*
             * if PeR(j) >= PeR(i)
             */
            // Forward(m,j)
            // Hanya kirim pesan ke tetangga jika tetangga tersebut lebih 'populer'. default 20% value
            return otherEngine.getRank() >= (this.myRank * rankThreshold);
        }

        return false;
    }
    // @Override
    // public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost
    // thisHost) {
    // // Jika bertemu langsung dengan pemilik pesan, langsung kirim
    // if (m.getTo().equals(otherHost)) {
    // return true;
    // }

    // MessageRouter otherRouter = otherHost.getRouter();

    // if (otherRouter instanceof DecisionEngineRouter) {
    // DecisionEngineRouter deRouter = (DecisionEngineRouter) otherRouter;
    // RoutingDecisionEngine engine = deRouter.getDecisionEngine();

    // if (engine instanceof PeopleRankEngine) {
    // PeopleRankEngine otherEngine = (PeopleRankEngine) engine;

    // /*
    // * Hanya kirim pesan ke tetangga jika tetangga tersebut lebih 'populer'
    // * atau memiliki konektivitas yang lebih tinggi (Rank lebih besar).
    // */
    // return otherEngine.getRank() > this.myRank;
    // }
    // }

    // Jika tetangga tidak punya engine PeopleRank, tolak
    // return false;

    // }

    /**
     * Mengembalikan nilai rank saat ini
     */
    public double getRank() {
        return this.myRank;
    }

    /**
     * Mengembalikan jumlah teman (neighbors) yang pernah bertemu
     */
    public int getFriendsCount() {
        return this.friends.size();
    }

    @Override
    public RoutingDecisionEngine replicate() {
        // Membuat salinan engine untuk node-node lain di simulator
        return new PeopleRankEngine(this);
    }

    private PeopleRankEngine getOtherEngine(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        if (otherRouter instanceof DecisionEngineRouter) {
            DecisionEngineRouter deRouter = (DecisionEngineRouter) otherRouter;
            RoutingDecisionEngine engine = deRouter.getDecisionEngine();
            if (engine instanceof PeopleRankEngine) {
                return (PeopleRankEngine) engine;
            }
        }
        return null;
    }

    public void connectionUp(DTNHost thisHost, DTNHost peer) {
    }

    public void connectionDown(DTNHost thisHost, DTNHost peer) {
    }

    public boolean newMessage(Message m) {
        m.addProperty(MSG_PEOPLERANK_PROP, 0.0);
        return true;
    }

    public boolean isFinalDest(Message m, DTNHost aHost) {
        return m.getTo().equals(aHost);
    }

    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        return true;
    }

    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        return false;
    }

    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        return true;
    }

    public void update(DTNHost thisHost) {
    }
}