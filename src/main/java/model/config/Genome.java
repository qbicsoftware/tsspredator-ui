package model.config;

import java.util.ArrayList;

/**
 * Contains all the information about a genome that are written to the config file
 * @author jmueller
 */
public class Genome {
    private String name;
    private String fasta; //Only set if it's a genome and not a condition
    private String gff; //Only set if it's a genome and not a condition
    private String alignmentID;
    private ArrayList<Replicate> replicateList;

    public Genome(){
        replicateList = new ArrayList<>();

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFasta() {
        return fasta;
    }

    public void setFasta(String fasta) {
        this.fasta = fasta;
    }

    public String getAlignmentID() {
        return alignmentID;
    }

    public void setAlignmentID(String alignmentID) {
        this.alignmentID = alignmentID;
    }

    public String getGff() {
        return gff;
    }

    public void setGff(String gff) {
        this.gff = gff;
    }

    public ArrayList<Replicate> getReplicateList() {
        return replicateList;
    }

}
