public class Timeout {

    private final static double A = 0.875;
    private final static double B = 0.75;

    private long ertt;
    private long edev;
    private long to;

    public Timeout(long to) {
        this.ertt = 0;
        this.edev = 0;
        this.to = to;
    }

    public void update(int seq, long timestamp) {
        if (seq == 0) {
            this.ertt = System.nanoTime() - timestamp;
            this.edev = 0;
            this.to = 2 * ertt;
        } else {
            long srtt = System.nanoTime() - timestamp;
            long sdev = Math.abs(srtt - ertt);
            this.ertt = (long)(A * this.ertt + (1 - A) * srtt);
            this.edev = (long)(B * this.edev + (1 - B) * sdev);
            this.to = this.ertt + 4 * this.edev;
        }
    }

    public long getTo() {
        return this.to;
    }
}