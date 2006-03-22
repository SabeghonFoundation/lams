package org.lamsfoundation.lams.tool.example;



/**
 * 
 * Caches the user details. This allows the tool to be more efficient at 
 * displaying user names but means that when people's names change, they
 * won't change in the "old" tool data.
 * 
 *        @hibernate.class
 *         table="tl_laex11_user"
 *     
 */

public class ExampleUser  implements java.io.Serializable {


    // Fields    
     private Long uid;
     private Long userId;
     private String lastName;
     private String firstName;
     private ExampleSession exampleSession;


    // Constructors

    /** default constructor */
    public ExampleUser() {
    }

    
    /** full constructor */
    public ExampleUser(Long userId, String lastName, String firstName, ExampleSession exampleSession) {
        this.userId = userId;
        this.lastName = lastName;
        this.firstName = firstName;
        this.exampleSession = exampleSession;
    }

   
    // Property accessors
    /**       
     *            @hibernate.id
     *             generator-class="native"
     *             type="java.lang.Long"
     *             column="uid"
     */

    public Long getUid() {
        return this.uid;
    }
    
    public void setUid(Long uid) {
        this.uid = uid;
    }
    /**       
     *            @hibernate.property
     *             column="user_id"
     *             length="20"
     *         
     */

    public Long getUserId() {
        return this.userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    /**       
     *            @hibernate.property
     *             column="last_name"
     *             length="255"
     *         
     */

    public String getLastName() {
        return this.lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    /**       
     *            @hibernate.property
     *             column="first_name"
     *             length="255"
     *         
     */

    public String getFirstName() {
        return this.firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    /**       
     *            @hibernate.many-to-one
     *             not-null="true"
     *            @hibernate.column name="session_id"         
     *         
     */

    public ExampleSession getExampleSession() {
        return this.exampleSession;
    }
    
    public void setExampleSession(ExampleSession exampleSession) {
        this.exampleSession = exampleSession;
    }
   

    /**
     * toString
     * @return String
     */
     public String toString() {
	  StringBuffer buffer = new StringBuffer();

      buffer.append(getClass().getName()).append("@").append(Integer.toHexString(hashCode())).append(" [");
      buffer.append("userId").append("='").append(getUserId()).append("' ");			
      buffer.append("]");
      
      return buffer.toString();
     }


   public boolean equals(Object other) {
         if ( (this == other ) ) return true;
		 if ( (other == null ) ) return false;
		 if ( !(other instanceof ExampleUser) ) return false;
		 ExampleUser castOther = ( ExampleUser ) other; 
         
		 return ( (this.getUid()==castOther.getUid()) || ( this.getUid()!=null && castOther.getUid()!=null && this.getUid().equals(castOther.getUid()) ) );
   }
   
   public int hashCode() {
         int result = 17;
         result = 37 * result + ( getUid() == null ? 0 : this.getUid().hashCode() );
         return result;
   }   





}