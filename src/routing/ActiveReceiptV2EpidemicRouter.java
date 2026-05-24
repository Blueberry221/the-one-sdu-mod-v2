package routing;

import java.util.HashSet;
import java.util.Set;

import core.DTNHost;
import core.Message;
import core.Settings;

/**
 * Router yang mengimplementasikan mekanisme Active Receipt V2 (ACK + HashSet)
 * menggunakan pendekatan intervensi checkReceiving dan messageTransferred.
 * * Karakteristik V2:
 * - Pengecekan status kekebalan dilakukan di checkReceiving untuk menghemat bandwidth.
 * - Eksekusi pembersihan pesan usang dan pencarian kecocokan ID dilakukan pasca-transfer 
 * di dalam messageTransferred guna menjaga kestabilan penyebaran multi-hop di jaringan.
 */
public class ActiveReceiptV2EpidemicRouter extends EpidemicRouter {

    public static final String RECEIPT_PREFIX = "R_";
    private static final String ACK_TTL = "ack_ttl";
    private static final int RECEIPT_SIZE = 1;

    // Struktur data HashSet untuk menyimpan daftar ID pesan yang sudah selesai/sembuh di level JVM
    private Set<String> curedMessages;
    private int ackTtl;

    public ActiveReceiptV2EpidemicRouter(Settings s) {
        super(s);
        this.curedMessages = new HashSet<String>();

        // Membaca konfigurasi nilai TTL untuk pesan tanda terima jika didefinisikan di file .cfg
        if (s.contains(ACK_TTL)) {
            this.ackTtl = s.getInt(ACK_TTL);
            if (ackTtl <= 0) {
                throw new IllegalArgumentException("Ack TTL must be positive");
            }
        } else {
            // Nilai default jika parameter ack_ttl tidak ditemukan di berkas .cfg
            this.ackTtl = 300; 
        }
    }

    protected ActiveReceiptV2EpidemicRouter(ActiveReceiptV2EpidemicRouter r) {
        super(r);
        this.curedMessages = new HashSet<String>(r.curedMessages);
        this.ackTtl = r.ackTtl;
    }

    // =================================================================
    // GERBANG 1: FILTER AWAL SEBELUM PROSES TRANSFER DIMULAI
    // =================================================================
    @Override
    protected int checkReceiving(Message m) {
        String msgId = m.getId();
        
        // FASE 4: VAKSINASI
        // Jika objek yang masuk bukan tanda terima, tetapi ID-nya sudah ada di HashSet,
        // maka proses transfer langsung ditolak untuk menghemat bandwidth.
        if (!msgId.startsWith(RECEIPT_PREFIX) && curedMessages.contains(msgId)) {
            return DENIED_OLD;
        }

        return super.checkReceiving(m);
    }

    // =================================================================
    // GERBANG 2: LISTENERS PASCA-TRANSFER (DATA SUKSES MENDARAT 100%)
    // =================================================================
    @Override
    public Message messageTransferred(String id, DTNHost from) {
        // Memasukkan objek pesan secara legal ke dalam memori buffer node
        Message m = super.messageTransferred(id, from);
        String msgId = m.getId();

        // -------------------------------------------------------------
        // FASE 3: DETEKSI PESAN TANDA TERIMA & PEMBERSIHAN PASCA-TRANSFER
        // -------------------------------------------------------------
        if (msgId.startsWith(RECEIPT_PREFIX)) {
            String orgMsgId = msgId.substring(RECEIPT_PREFIX.length());

            // Menghapus pesan asli yang usang dari buffer jika objek tersebut ditemukan
            if (hasMessage(orgMsgId)) {
                deleteMessage(orgMsgId, false);
            }
            // Mencatat status ke dalam HashSet lokal node
            curedMessages.add(orgMsgId);
            return m;
        }
        
        // -------------------------------------------------------------
        // FASE 1: MENDARAT DI TUJUAN AKHIR & PENCIPTAAN TANDA TERIMA
        // -------------------------------------------------------------
        if (m.getTo() == getHost()) {
            String receiptId = RECEIPT_PREFIX + msgId;

            // Memastikan tanda terima fisik belum ada di buffer dan belum tercatat di HashSet
            if (!hasMessage(receiptId) && !curedMessages.contains(msgId)) {

                // Membuat objek pesan tanda terima baru berukuran 1 byte
                Message receiptMsg = new Message(getHost(), m.getFrom(), receiptId, RECEIPT_SIZE);
                receiptMsg.setTtl(ackTtl);

                // Memasukkan pesan tanda terima ke dalam struktur data pesan aktif milik router
                addToMessages(receiptMsg, true);

                // Menandai pesan asli sebagai pesan yang sudah selesai/sembuh
                curedMessages.add(msgId);
            
            }
        }
        return m;
    }

    @Override
    public ActiveReceiptV2EpidemicRouter replicate() {
        return new ActiveReceiptV2EpidemicRouter(this);
    }
}