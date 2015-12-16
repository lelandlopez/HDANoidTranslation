package com.hida;

import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An object used to model every Id. By definition, each Id will have a unique
 * name associated with it. However, to determine uniqueness, each newly created
 * Id must be compared to previously existing Ids.
 *
 * Comparisons will be made by using Sets collection. Depending on which set is
 * used, the Comparable interface and an overridden equals and hashCode methods
 * were overridden to accommodate.
 *
 * @author lruffin
 */
public abstract class Id implements Comparable<Id> {

    // fields
    private int[] BaseMap;
    private boolean Unique = true;
    private final String Prefix;
    
    // Logger; logfile to be stored in resource folder
    private static final Logger Logger = LoggerFactory.getLogger(Id.class);
    /**
     * Copy constructor; primarily used to copy values of the BaseMap from one
     * Id to another.
     *
     * @param id The Id to copy from.
     */
    public Id(Id id) {
        this.Prefix = id.Prefix;
        this.BaseMap = Arrays.copyOf(id.getBaseMap(), id.getBaseMap().length);
        this.Unique = id.Unique;
    }

    public Id(int[] baseMap, String Prefix) {
        this.BaseMap = Arrays.copyOf(baseMap, baseMap.length);
        this.Prefix = Prefix;
    }

    public abstract boolean incrementId();

    public abstract String getRootName();

    
    @Override
    public int hashCode() {
        // arbitrarily chosen prime numbers
        final int prime1 = 37;
        final int prime2 = 257;

        int hash = prime1 * prime2 + Arrays.hashCode(this.BaseMap);
        return hash;
    }

    /**
     * Overridden so that id's can be identified solely by its baseName.
     *
     * @param obj the Object this id is being compared to
     * @return true if the two Objects are the same.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Id)) {
            return false;
        }
        final Id paramId = (Id) obj;

        return Arrays.equals(this.BaseMap, paramId.BaseMap);
    }

    /**
     * The BaseMap is the numerical representation of this Id's name. Used in
     * conjunction with TokenMap to return the string representation of this
     * id's name in its toString method.
     *
     * @return The array used to create the name
     */
    public int[] getBaseMap() {
        return BaseMap;
    }

    /**
     * The BaseMap is the numerical representation of this Id's name. Used in
     * conjunction with TokenMap to return the string representation of this
     * id's name in its toString method.
     *
     * Be warned that the array must have a unique address for it to work with
     * Sets. The length of the array must be equal to TokenMap, otherwise an
     * IndexOutOfBounds error will be thrown in the getRootName method.
     *
     * @param baseMap The new array to replace the name.
     */
    public void setBaseMap(int[] baseMap) {
        this.BaseMap = baseMap;
    }

    /**
     * Determines whether or not particular Id is not the first to be created
     * with it's particular BaseMap.
     *
     * Returns true by default unless previously modified.
     *
     * @return
     */
    public boolean isUnique() {
        return Unique;
    }

    /**
     * Determines whether or not particular Id is not the first to be created
     * with it's particular BaseMap.
     *
     * Should only be used to set false whenever an it is determined that this
     * Id is not the first to have it's BaseMap value.
     *
     * @param isUnique
     */
    public void setUnique(boolean isUnique) {
        this.Unique = isUnique;
    }

    /**
     * Method to retrieve the prefix of the id
     * @return 
     */
    public String getPrefix() {
        return Prefix;
    }

    /**
     * Used to define the natural ordering of how id's should be listed. When
     * invoked, the two id's will be compared by their arrays as they represent
     * the names.
     *
     * @param t second Id being compared.
     * @return used to sort values in descending order.
     */
    @Override
    public int compareTo(Id t) {
        int[] t1Array = this.getBaseMap();
        int[] t2Array = t.getBaseMap();
        if (this.equals(t)) {
            return 0;
        } else {
            for (int i = 0; i < t1Array.length; i++) {
                // if the first Id has a smaller value than the second Id 
                if (t1Array[i] < t2Array[i]) {
                    return -1;
                } // if the first Id has a larger value than the second Id
                else if (t1Array[i] > t2Array[i]) {
                    return 1;
                }
            }
        }
        // if the arrays of both Ids are equal
        return 0;
    }
}
