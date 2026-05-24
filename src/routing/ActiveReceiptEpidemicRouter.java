package routing;

import core.*;

/**
 * Router ini mengimplementasikan mekanisme Active Receipt untuk mencegah re-infeksi 
 * pada node yang telah disembuhkan dengan pendekatan murni berbasis Prefix (Pure Prefix).
 * * Karakteristik V1 (Pure Prefix):
 * 1. Menggunakan prefix (awalan "R_") untuk membedakan pesan tanda terima (receipt) dengan pesan asli.
 * 2. Memanfaatkan satu buffer utama (tas ransel bawaan simulator) yang sama untuk menyimpan kedua jenis pesan tersebut.
 * 3. Status "Kekebalan" node bersifat SEMENTARA. Jika pesan tanda terima (receipt) dihapus dari buffer 
 * karena batasan kapasitas (buffer full) atau masa berlaku habis (TTL expired), maka node berpotensi 
 * mengalami "amnesia" dan dapat terinfeksi ulang oleh pesan lama.
 * 4. Pendekatan ini sangat hemat penggunaan memori internal Java (JVM) karena tidak membuat struktur data baru,
 * namun manajemen kekebalannya sangat terikat pada siklus hidup pesan di dalam buffer simulator.
 */
public class ActiveReceiptEpidemicRouter extends EpidemicRouter {

    /* * [PENJELASAN TEKNIS 1: SISTEM PELABELAN]
     * Di simulator, pesan asli dan pesan vaksin sama-sama berwujud objek 'Message'.
     * Jadi tidak membutuhkan Hashmap baru, melainkan pesan vaksin akan disimpan di buffer bawaan
     * Kita butuh cara agar router bisa membedakan keduanya.
     * Oleh karena itu, kita membuat prefix "R_".
     * Jika pesan asli namanya "M1", maka vaksinnya bernama "R_M1".
     */
    public static final String RECEIPT_PREFIX = "R_";

    public ActiveReceiptEpidemicRouter(Settings s) {
        super(s);
    }

    protected ActiveReceiptEpidemicRouter(ActiveReceiptEpidemicRouter r) {
        super(r);
    }





    
    @Override
    public int receiveMessage(Message m, DTNHost from) {
        String messageId = m.getId();

        // =================================================================
        // FASE 3: DETEKSI DAN PEMBERSIHAN 
        // =================================================================
        // Router mengecek: "Apakah pesan yang datang ini nama depannya R_?"
        if (messageId.startsWith(RECEIPT_PREFIX)) {

            /*
             * [PENJELASAN TEKNIS 2: MANIPULASI STRING]
             * Karena namanya "R_M1", kita harus memotong teks "R_"-nya
             * agar kita tahu bahwa target yang harus diobati/dihapus adalah "M1".
             */
            String originalId = messageId.substring(RECEIPT_PREFIX.length());

            // Cek tas ransel (buffer): "Apakah saya masih nyimpen M1?"
            if (this.hasMessage(originalId)) {
                // Kalau ada, hapus M1 dari memori! (Node berhasil disembuhkan)
                this.deleteMessage(originalId, false);
            }

            // Masukkan pesan OBAT ini ke tas ransel.
            // Karena ini turunan EpidemicRouter, obat ini otomatis akan
            // disebarkan ke node lain yang berpapasan secara aktif (Fase 2).
            return super.receiveMessage(m, from);
        }

        // =================================================================
        // FASE 4: MENCEGAH RE-INFEKSI / VAKSIN
        // =================================================================
        // Bikin prediksi nama obatnya (misal yang mau masuk "M1", berarti obatnya
        // "R_M1")
        String expectedReceiptId = RECEIPT_PREFIX + messageId;

        // Cek tas ransel: "Apakah saya sudah punya obat R_M1?"
        if (this.hasMessage(expectedReceiptId)) {

            /*
             * [PENJELASAN TEKNIS 4: SISTEM PENOLAKAN RESMI]
             * Jika sudah punya obatnya, berarti pesan M1 itu sudah basi.
             * Kita kembalikan status DENIED_OLD agar node pengirim membatalkan
             * transfer Bluetooth-nya. Ini sangat menghemat bandwidth jaringan!
             */
            return MessageRouter.DENIED_OLD;
        }

        // =================================================================
        // PROSES PENERIMAAN PESAN BIASA
        // =================================================================
        // Biarkan router memproses pesan seperti biasa (menyalin ke buffer)
        int receiveResult = super.receiveMessage(m, from);

        // =================================================================
        // FASE 1: MENDARAT DI TUJUAN AKHIR & PENCIPTAAN OBAT
        // =================================================================
        /*
         * [PENJELASAN TEKNIS 5: PENGECEKAN RCV_OK]
         * receiveResult == MessageRouter.RCV_OK memastikan bahwa transfer
         * Bluetooth via udara benar-benar selesai 100% tanpa terputus.
         * m.getTo() == getHost() memastikan bahwa node ini adalah Tujuan Akhirnya.
         */
        if (receiveResult == MessageRouter.RCV_OK && m.getTo() == getHost()) {

            // Pastikan kita belum pernah bikin obat untuk pesan ini sebelumnya
            if (!this.hasMessage(expectedReceiptId)) {

                /*
                 * [PENJELASAN TEKNIS 3: UKURAN OBAT SUPER KECIL]
                 * Sesuai teori, cost transmisi receipt jauh lebih kecil dari pesan asli.
                 * Kita paksa ukurannya menjadi 1 byte saja (sangat kecil),
                 * sehingga tidak akan membuat tas ransel (buffer) node kepenuhan
                 * meski mereka membawa ribuan obat sekaligus.
                 */
                int receiptSize = 1;

                // Ciptakan objek pesan fisik baru yang berfungsi sebagai OBAT
                Message receipt = new Message(getHost(), m.getFrom(), expectedReceiptId, receiptSize);

                // Masukkan OBAT ke dalam tas utama (buffer) milik router ini.
                // EpidemicRouter akan otomatis menyebarkan obat ini ke jaringan!
                this.createNewMessage(receipt);
            }
        }

        // Kembalikan status hasil penerimaan pesan aslinya ke simulator
        return receiveResult;
    }

    @Override
    public ActiveReceiptEpidemicRouter replicate() {
        return new ActiveReceiptEpidemicRouter(this);
    }
}
