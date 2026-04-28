/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package routing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;

/**
 * Implementation of Spray and wait router as depicted in
 * <I>Spray and Wait: An Efficient Routing Scheme for Intermittently
 * Connected Mobile Networks</I> by Thrasyvoulos Spyropoulus et al.
 *
 */
public class SprayAndFocusRouter extends ActiveRouter {
	/** identifier for the initial number of copies setting ({@value}) */
	public static final String NROF_COPIES = "nrofCopies";
	/** identifier for the binary-mode setting ({@value}) */
	public static final String BINARY_MODE = "binaryMode";
	/** SprayAndWait router's settings name space ({@value}) */
	public static final String SPRAYANDWAIT_NS = "SprayAndWaitRouter";
	/** Message property key */
	public static final String MSG_COUNT_PROPERTY = SPRAYANDWAIT_NS + "." +
			"copies";

	protected Map<DTNHost, Double> lastEncounterTimes; // Mencatat waktu pertemuan terakhir
	protected double transitivityTimerThreshold = 60.0; // Ambang batas waktu
	protected int initialNrofCopies;
	protected boolean isBinary;

	public SprayAndFocusRouter(Settings s) {
		super(s);
		Settings snwSettings = new Settings(SPRAYANDWAIT_NS);

		initialNrofCopies = snwSettings.getInt(NROF_COPIES);
		isBinary = snwSettings.getBoolean(BINARY_MODE);
	}

	/**
	 * Copy constructor.
	 * 
	 * @param r The router prototype where setting values are copied from
	 */
	protected SprayAndFocusRouter(SprayAndFocusRouter r) {
		super(r);
		this.initialNrofCopies = r.initialNrofCopies;
		this.isBinary = r.isBinary;
	}

	@Override
	public void changedConnection(Connection con) {
		super.changedConnection(con);
		if (con.isUp()) {
			DTNHost peer = con.getOtherNode(getHost());
			lastEncounterTimes.put(peer, SimClock.getTime());
		}
	}

	@Override
	public int receiveMessage(Message m, DTNHost from) {
		return super.receiveMessage(m, from);
	}

	@Override
	public Message messageTransferred(String id, DTNHost from) {
		Message msg = super.messageTransferred(id, from);
		Integer nrofCopies = (Integer) msg.getProperty(MSG_COUNT_PROPERTY);

		assert nrofCopies != null : "Not a SnW message: " + msg;

		if (isBinary) {
			/* in binary S'n'W the receiving node gets ceil(n/2) copies */
			nrofCopies = (int) Math.ceil(nrofCopies / 2.0);
		} else {
			/* in standard S'n'W the receiving node gets only single copy */
			nrofCopies = 1;
		}

		msg.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);
		return msg;
	}

	@Override
	public boolean createNewMessage(Message msg) {
		makeRoomForNewMessage(msg.getSize());

		msg.setTtl(this.msgTtl);
		msg.addProperty(MSG_COUNT_PROPERTY, new Integer(initialNrofCopies));
		addToMessages(msg, true);
		return true;
	}

	@Override
	public void update() {
		super.update();
		if (!canStartTransfer() || isTransferring())
			return;

		if (exchangeDeliverableMessages() != null)
			return;

		// Gunakan salinan list pesan agar tidak error saat pesan berpindah/dihapus
		List<Message> msgList = new ArrayList<Message>(getMessageCollection());

		for (Message m : msgList) {
			// Jika sedang transfer pesan sebelumnya, berhenti dulu agar tidak bentrok
			if (isTransferring())
				break;

			Integer nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROPERTY);
			if (nrofCopies == null)
				continue;

			if (nrofCopies > 1) {
				// --- FASE SPRAY ---
				// Kirim ke koneksi mana saja (Binary/Linear Spray)
				tryMessagesToConnections(Collections.singletonList(m), getConnections());
			} else {
				// --- FASE FOCUS ---
				DTNHost dest = m.getTo();
				double myLastSeen = lastEncounterTimes.getOrDefault(dest, 0.0);

				for (Connection c : getConnections()) {
					DTNHost peer = c.getOtherNode(getHost());
					MessageRouter peerRouter = peer.getRouter();

					// Pastikan tetangga juga punya fitur Focus (catatan waktu)
					if (peerRouter instanceof SprayAndFocusRouter) {
						double peerLastSeen = ((SprayAndFocusRouter) peerRouter).getEncounterTime(dest);

						// Bandingkan: Apakah tetangga melihat target lebih baru dibanding kita?
						// TIMER_THRESHOLD biasanya 300 detik (5 menit) atau sesuaikan kebutuhan
						if (peerLastSeen > myLastSeen + 300) {
							if (startTransfer(m, c) == 0) {
								break;
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Creates and returns a list of messages this router is currently
	 * carrying and still has copies left to distribute (nrof copies > 1).
	 * 
	 * @return A list of messages that have copies left
	 */
	protected List<Message> getMessagesWithCopiesLeft() {
		List<Message> list = new ArrayList<Message>();

		for (Message m : getMessageCollection()) {
			Integer nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROPERTY);
			assert nrofCopies != null : "SnW message " + m + " didn't have " +
					"nrof copies property!";
			if (nrofCopies > 1) {
				list.add(m);
			}
		}

		return list;
	}

	/**
	 * Called just before a transfer is finalized (by
	 * {@link ActiveRouter#update()}).
	 * Reduces the number of copies we have left for a message.
	 * In binary Spray and Wait, sending host is left with floor(n/2) copies,
	 * but in standard mode, nrof copies left is reduced by one.
	 */
	@Override
	protected void transferDone(Connection con) {
		Integer nrofCopies;
		String msgId = con.getMessage().getId();
		/* get this router's copy of the message */
		Message msg = getMessage(msgId);

		if (msg == null) { // message has been dropped from the buffer after..
			return; // ..start of transfer -> no need to reduce amount of copies
		}

		/* reduce the amount of copies left */
		nrofCopies = (Integer) msg.getProperty(MSG_COUNT_PROPERTY);
		if (isBinary) {
			nrofCopies /= 2;
		} else {
			nrofCopies--;
		}
		msg.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);
	}

	/**
	 * Helper untuk mengambil waktu pertemuan terakhir
	 */
	public double getEncounterTime(DTNHost host) {
		if (lastEncounterTimes.containsKey(host)) {
			return lastEncounterTimes.get(host);
		}
		return 0.0;
	}

	@Override
	public SprayAndFocusRouter replicate() {
		return new SprayAndFocusRouter(this);
	}
}
