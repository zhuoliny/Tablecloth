package edu.buffalo.tablecloth.data;

/**
 * Created by jbueno on 3/24/16.
 */
public class CanData {
    private float accX,accY,accZ, magX,magY, magZ, gyrX, gyrY,gyrZ;
    private String yymmdd, hhssmm;
    private long id;

    public CanData(float accX,
                   float accY,
                   float accZ,
                   float magX,
                   float magY,
                   float magZ,
                   float gyrX,
                   float gyrY,
                   float gyrZ,
                   String yymmdd, //year month day
                   String hhssmm // hour, second, microsecond
    ) {
        this.accX = accX;
        this.accY = accY;
        this.accZ = accZ;
        this.magX = magX;
        this.magY = magY;
        this.magZ = magZ;
        this.gyrX = gyrX;
        this.gyrY = gyrY;
        this.gyrZ = gyrZ;
        this.yymmdd = yymmdd;
        this.hhssmm = hhssmm;
    }


    public void setID(long id) { this.id = id;}
    public long getID() { return this.id;}
    public String toString() {
        return accX + "," + accY+","+accZ
                +","+magX+","+magY+","+magZ
                +","+gyrX+","+gyrY+","+gyrZ+","+yymmdd+","+hhssmm;
    }
}
