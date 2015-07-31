package ch.usi.da.paxos.storage;
/* 
 * Copyright (c) 2013 Università della Svizzera italiana (USI)
 * 
 * This file is part of URingPaxos.
 *
 * URingPaxos is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * URingPaxos is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with URingPaxos.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.Serializable;

import ch.usi.da.paxos.message.Value;

/**
 * Name: Decision<br>
 * Description: <br>
 * 
 * Creation date: Apr 2, 2012<br>
 * $Id$
 * 
 * @author Samuel Benz benz@geoid.ch
 */
public class Decision implements Serializable {
	
	private static final long serialVersionUID = -2916694736282875646L;
	
	public static final Decision SKIP = new Decision(null, null, null, null);

	private final Integer ring;
	
	private final Long instance;
	
	private final Integer ballot;
	
	private final Value value;
	
	private long  valueCounterOfFirstValueInRing = -1; // starts at 1 in the ring
	
	private long  valueCounterInInstance = -1; // starts at 1 in the instance
	
	/**
	 * @param ring
	 * @param instance
	 * @param ballot
	 * @param value
	 */
	public Decision(Integer ring,Long instance,Integer ballot,Value value){
		this.ring = ring;
		this.instance = instance;
		this.ballot = ballot;
		this.value = value;
	}

	/**
	 * @return the ring
	 */
	public Integer getRing() {
		return ring;
	}

	/**
	 * @return the instance
	 */
	public Long getInstance() {
		return instance;
	}

	/**
	 * @return the ballot
	 */
	public Integer getBallot() {
		return ballot;
	}

	/**
	 * @return the value
	 */
	public Value getValue() {
		return value;
	}
	
	public void setInstanceValueCounter(long v) {
	   this.valueCounterInInstance = v;
	}
	
   public long getInstanceValueCounter() {
      return this.valueCounterInInstance;
   }
	
	/** Sets the value counter of the first value contained in this instance, no matter
	 *  how many values were decided together, with regards to the ring where it was
	 *  decided. For example, if the first value of this Decision is the <b>i-th</b>
	 *  value decided in this ring, it must be set by the MultiLearnerRole as <b>i</i>.
	 *  The value counter is the one used by the deterministic merge.
	 * 
	 * @param v
	 */
	public void setRingValueCounter(long v) {
	   this.valueCounterOfFirstValueInRing = v;
	}
	
   /** Gets the value counter of the first value contained in this instance, no matter
    *  how many values were decided together, with regards to the ring where it was
    *  decided. For example, if the first value of this Decision is the <b>i-th</b>
    *  value decided in this ring, this method returns <b>i</i>. The value counter is
    *  the one used by the deterministic merge.
    *  
	 * @return the counter of the first value contained in this decision.
	 */
	public long getRingValueCounter() {
	   return this.valueCounterOfFirstValueInRing;
	}
	
	public boolean isSkip() {
	   return this == Decision.SKIP || this.getValue().isSkip();
	}
	
	public long getNumberOfSkips() {
	   long skips = 0;
	   if (isSkip()) {
         skips = Long.parseLong(new String(getValue().getValue()));
	   }
	   else {
         System.err.println("Decision is not a skip!");
         System.exit(1);
	   }
	   return skips;
	}
	
	public String toString(){
		return("decided to: " + this.getValue() + " (ring:" + this.getRing() + " instance:" + this.getInstance() + " ballet:" + this.getBallot() + ")");
	}
	
	public boolean equals(Object obj) {
		if(obj instanceof Decision){
			Decision d = (Decision) obj;
            if(this.getRing().equals(d.getRing()) && this.getInstance().equals(d.getInstance()) && this.getValue().equals(d.getValue())){
                    return true;
            }
		}
		return false;
	}
	
	public int hashCode() {
		return this.getInstance().intValue();
	}	
}
