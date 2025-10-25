package models;

import java.sql.Timestamp;

/**
 * PrintJob model representing a print request in the queue
 */
public class PrintJob {
    private int jobId;
    private int userId;
    private String documentName;
    private byte[] documentContent;
    private String documentPath;
    private int pageCount;
    private int numCopies;
    private double totalCost;
    private JobStatus jobStatus;
    private PaymentStatus paymentStatus;
    private PaymentType paymentType;
    private int queuePosition;
    private Timestamp submittedAt;
    private Timestamp startedAt;
    private Timestamp completedAt;
    private Integer operatorId;
    private String notes;
    
    // Additional fields for display purposes
    private String username;
    private String fullName;
    
    public enum JobStatus {
        PENDING, PROCESSING, COMPLETED, CANCELLED
    }
    
    public enum PaymentStatus {
        UNPAID, PAID, REFUNDED
    }
    
    public enum PaymentType {
        PREPAID, POSTPAID
    }
    
    // Constructors
    public PrintJob() {}
    
    public PrintJob(int userId, String documentName, int pageCount, int numCopies, 
                    double totalCost, PaymentType paymentType) {
        this.userId = userId;
        this.documentName = documentName;
        this.pageCount = pageCount;
        this.numCopies = numCopies;
        this.totalCost = totalCost;
        this.paymentType = paymentType;
        this.jobStatus = JobStatus.PENDING;
        this.paymentStatus = (paymentType == PaymentType.PREPAID) ? PaymentStatus.PAID : PaymentStatus.UNPAID;
    }
    
    // Getters and Setters
    public int getJobId() {
        return jobId;
    }
    
    public void setJobId(int jobId) {
        this.jobId = jobId;
    }
    
    public int getUserId() {
        return userId;
    }
    
    public void setUserId(int userId) {
        this.userId = userId;
    }
    
    public String getDocumentName() {
        return documentName;
    }
    
    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }
    
    public byte[] getDocumentContent() {
        return documentContent;
    }
    
    public void setDocumentContent(byte[] documentContent) {
        this.documentContent = documentContent;
    }
    
    public String getDocumentPath() {
        return documentPath;
    }
    
    public void setDocumentPath(String documentPath) {
        this.documentPath = documentPath;
    }
    
    public int getPageCount() {
        return pageCount;
    }
    
    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }
    
    public int getNumCopies() {
        return numCopies;
    }
    
    public void setNumCopies(int numCopies) {
        this.numCopies = numCopies;
    }
    
    public double getTotalCost() {
        return totalCost;
    }
    
    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }
    
    public JobStatus getJobStatus() {
        return jobStatus;
    }
    
    public void setJobStatus(JobStatus jobStatus) {
        this.jobStatus = jobStatus;
    }
    
    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }
    
    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }
    
    public PaymentType getPaymentType() {
        return paymentType;
    }
    
    public void setPaymentType(PaymentType paymentType) {
        this.paymentType = paymentType;
    }
    
    public int getQueuePosition() {
        return queuePosition;
    }
    
    public void setQueuePosition(int queuePosition) {
        this.queuePosition = queuePosition;
    }
    
    public Timestamp getSubmittedAt() {
        return submittedAt;
    }
    
    public void setSubmittedAt(Timestamp submittedAt) {
        this.submittedAt = submittedAt;
    }
    
    public Timestamp getStartedAt() {
        return startedAt;
    }
    
    public void setStartedAt(Timestamp startedAt) {
        this.startedAt = startedAt;
    }
    
    public Timestamp getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(Timestamp completedAt) {
        this.completedAt = completedAt;
    }
    
    public Integer getOperatorId() {
        return operatorId;
    }
    
    public void setOperatorId(Integer operatorId) {
        this.operatorId = operatorId;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    
    @Override
    public String toString() {
        return "PrintJob{" +
                "jobId=" + jobId +
                ", documentName='" + documentName + '\'' +
                ", pageCount=" + pageCount +
                ", numCopies=" + numCopies +
                ", totalCost=" + totalCost +
                ", jobStatus=" + jobStatus +
                ", paymentStatus=" + paymentStatus +
                ", queuePosition=" + queuePosition +
                '}';
    }
}
