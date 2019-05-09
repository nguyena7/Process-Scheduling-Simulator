package project;

import java.lang.String;

public class PCB {
	private char event;
	private int arriveTime;
	private double startTime;
	private int waitTime;
	private int jobNum;
	private int memoryNeed;
	private int burstTime;
	private int burstIO;
	private int remainingBurstTime;
	private double completionTime;
	private boolean fromRQ1;
	private double ioStartTime;
	private double ioCompletionTime;
	private boolean fromIOWaitQueue;
	private int burstIORemain;
	private double jobSchedWaitTime;

	public PCB(String[] command) {
		// String[] parseStr = command.split(" | ");
		event = command[0].charAt(0);
		arriveTime = Integer.parseInt(command[1]);
		jobNum = Integer.parseInt(command[2]);
		memoryNeed = Integer.parseInt(command[3]);
		burstTime = Integer.parseInt(command[4]);
		remainingBurstTime = burstTime;
		fromIOWaitQueue = false;
	}

	public char getEvent() {
		return this.event;
	}

	public int getArriveTime() {
		return this.arriveTime;
	}

	public int getJobNum() {
		return this.jobNum;
	}

	public int getMemoryNeed() {
		return this.memoryNeed;
	}

	public int getBurstTime() {
		return this.burstTime;
	}

	public void setStartTime(double systemCounter) {
		this.startTime = systemCounter;
	}

	public double getStartTime() {
		return this.startTime;
	}

	public void setRemainingBurstTime(int remainingBT) {
		this.remainingBurstTime = remainingBT;
	}

	public int getRemainingBurstTime() {
		return remainingBurstTime;
	}

	public void setFromRQ1(boolean choice) { // true -> the process came from ready queue 1. false -> process came from
												// ready queue 2
		fromRQ1 = choice;
	}

	public boolean isFromRQ1() { // used to determine which queue a process came from
		return fromRQ1;
	}

	public void setWaitTime(int waitTime) {
		this.waitTime = waitTime;
	}

	public int getWaitTime() {
		return this.waitTime;
	}

	public void setCompletionTime(double newCompletionTime) {
		this.completionTime = newCompletionTime;
	}

	public double getCompletionTime() {
		return completionTime;
	}

	public void setBurstIO(int burstIO) {
		this.burstIO = burstIO;
		burstIORemain = burstIO;
	}

	public int getBurstIO() {
		return this.burstIO;
	}

	public void setBurstIORemain(int burstIORemain) {
		this.burstIORemain = burstIORemain;
	}

	public int getBurstIORemain() {
		return burstIORemain;
	}

	public double getIOStartTime() {
		return this.ioStartTime;
	}

	public void setIOStartTime(double newIOStart) {
		this.ioStartTime = newIOStart;
	}

	public double getIOCompletion() {
		return ioStartTime + burstIO;
	}

	public void setIOCompletion(double newIODoneTime) {
		this.ioCompletionTime = newIODoneTime;
	}

	public void setFromIOQueue(boolean choice) {
		this.fromIOWaitQueue = choice;
	}

	public boolean getFromIOQueue() {
		return fromIOWaitQueue;
	}

	public double getTurnAround() {
		return completionTime - arriveTime;
	}

	public void setJobSchWaitTime(double setJobSchWT) {
		this.jobSchedWaitTime = setJobSchWT;
	}

	public double getJobSchWaitTime() {
		return jobSchedWaitTime;
	}

	public void printPCB() {
		System.out.println("-----------------------------");
		System.out.println("event: " + event);
		System.out.println("arriveTime: " + arriveTime);
		System.out.println("jobNum: " + jobNum);
		System.out.println("memoryNeed: " + memoryNeed);
		System.out.println("burstTime: " + burstTime);
		System.out.println("-----------------------------");
	}
}
