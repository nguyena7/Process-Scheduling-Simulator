package project;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Comparator;
//import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import java.util.PriorityQueue;

public class RoundRobin {
	private int quantum1;
	private int quantum2;
	private final int MAX_MEM = 512;
	private int currentMem = 512;
	private int arriveJobCount = 0;
	private int processedJobsCount = 0;
	private int currBT;
	private double systemCounter = 0;
	private int arriveIO;
	private int requestIOCounter;
	PCB CPU[] = new PCB[1];
	PCB prevJobOnCPU;
	Queue<PCB> jobQueue = new LinkedList<PCB>();
	Queue<PCB> readyQueue1 = new LinkedList<PCB>();
	Queue<PCB> readyQueue2 = new LinkedList<PCB>();
	Queue<PCB> finishedQueue = new LinkedList<PCB>();
	Comparator<PCB> ioComparator = new IOBurstComparator();
	PriorityQueue<PCB> ioWaitQueue = new PriorityQueue<PCB>(ioComparator);
	PriorityQueue<PCB> ioWaitQueue2 = new PriorityQueue<PCB>(ioComparator);
	//Scanner input = new Scanner(System.in);

	public void rrRun() throws IOException {	
		BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
		String processCmd;

		while (true) { // input.hasNext()
			processCmd = input.readLine();
			System.out.println(processCmd);
			processInput(processCmd);
		}
		
		//displayFinished();
	}

	private void arrive(PCB job) {
		arriveJobCount++;

		while (true) { //systemCounter != job.getArriveTime()
			System.out.println("systemCounter: " + systemCounter + ", currentJobArriveTime: " + job.getArriveTime());
			systemCounter++;
			runProcess();
			if(systemCounter == job.getArriveTime() ) {
				System.out.printf("Event: A Time: %.0f Job: %d\n", systemCounter, job.getJobNum()); // debug

				if (job.getMemoryNeed() > MAX_MEM) {
					System.out.printf("Job %d exceeds the system's main memory capacity. TOSSED\n", job.getJobNum());
					return;
				}

				jobQueue.add(job);

				jobSchedule();

				processSchedule();
				break;
			}
			
		}
		
		/*System.out.printf("Event: A Time: %.0f Job: %d\n", systemCounter, job.getJobNum()); // debug

		if (job.getMemoryNeed() > MAX_MEM) {
			System.out.printf("Job %d exceeds the system's main memory capacity. TOSSED\n", job.getJobNum());
			return;
		}

		jobQueue.add(job);

		jobSchedule();

		processSchedule();
		*/
	}

	private void runProcessNoIOBurst() {
		System.out.println("runProcessNoIOBurst");
		if (CPU[0] != null) {
			System.out.println("Job " + CPU[0].getJobNum() + " RUNNING on CPU");
			currBT = CPU[0].getRemainingBurstTime();
			System.out.println("currBT: " + currBT);
			
			if (quantum1 == 0 || quantum2 == 0) {
				System.out.println("calling quantumExpire");
				quantumExpire();
			}
				
			if (currBT == 0) {
				System.out.println("currBT: " + currBT);
				jobTermination();
			}
			currBT--;
			
			if (CPU[0].isFromRQ1() == true && quantum1 != 0) {
				System.out.println("quantum1: " + quantum1);
				quantum1--;
				CPU[0].setRemainingBurstTime(currBT);
			} else if (CPU[0].isFromRQ1() == false && quantum2 != 0) {
				System.out.println("quantum2: " + quantum2);
				quantum2--;
				CPU[0].setRemainingBurstTime(currBT);
			} else {
				CPU[0].setRemainingBurstTime(currBT);
			}
		} else {
			System.out.println("CPU is empty in runProcess");
		}
	}
	
	private void runProcess() {
		System.out.println("runProcess");
		if (CPU[0] != null) {
			System.out.println("Job " + CPU[0].getJobNum() + " RUNNING on CPU");
			currBT = CPU[0].getRemainingBurstTime();
			System.out.println("currBT: " + currBT);
			currBT--;
			if (quantum1 == 0 || quantum2 == 0) {
				System.out.println("calling quantumExpire");
				quantumExpire();
			} else if (currBT == 0) {
				System.out.println("currBT: " + currBT);
				jobTermination();
			} else if (CPU[0].isFromRQ1() == true && quantum1 != 0) {
				System.out.println("quantum1: " + quantum1);
				quantum1--;
				CPU[0].setRemainingBurstTime(currBT);
			} else if (CPU[0].isFromRQ1() == false && quantum2 != 0) {
				System.out.println("quantum2: " + quantum2);
				quantum2--;
				CPU[0].setRemainingBurstTime(currBT);
			} else {
				CPU[0].setRemainingBurstTime(currBT);
			}
		} else {
			System.out.println("CPU is empty in runProcess");
		}
		
		runIOBurst();
	}

	public void runIOBurst() {
		if (ioWaitQueue.isEmpty()) {
			System.out.println("IO wait queue empty");
		} else if (!ioWaitQueue.isEmpty()) {
			// Iterator<PCB> ioQueueItr = ioWaitQueue.iterator();
			PCB processInIO;
			PriorityQueue<PCB> ioWaitQueue2 = new PriorityQueue<PCB>(ioComparator); // copy of ioWaitQueue for ordered
																					// iteration

			while (!ioWaitQueue.isEmpty()) {
				processInIO = ioWaitQueue.remove();
				System.out.printf("process %d IO Burst: %d w/ remaining BT: %d\n", processInIO.getJobNum(),
						processInIO.getBurstIORemain(), processInIO.getRemainingBurstTime());
				processInIO.setBurstIORemain(processInIO.getBurstIORemain() - 1);
				ioWaitQueue2.add(processInIO);
			}
			while (!ioWaitQueue2.isEmpty()) {
				ioWaitQueue.add(ioWaitQueue2.remove());
			}
			
			if (ioWaitQueue.peek().getBurstIORemain() == 0) {
				System.out.println("IO completed, calling completedIO()");
				completedIO();
			}
		}
	}
	
	private void jobSchedule() {
		// System.out.println("Inside jobSchedule");
		PCB job;
		while (jobQueue.peek() != null && jobQueue.peek().getMemoryNeed() <= currentMem) {
			// if(jobQueue.peek() != null && jobQueue.peek().getMemoryNeed() <= currentMem)
			// {
			job = jobQueue.remove();
			System.out.printf("job %d removed from jobQueue\n", job.getJobNum()); // debug

			readyQueue1.add(job);
			job.setFromRQ1(true);
			job.setJobSchWaitTime(systemCounter - job.getArriveTime());
			System.out.printf("job %d added to readyQueue1\n", job.getJobNum()); // debug

			currentMem -= job.getMemoryNeed();
			System.out.println("job memory size: " + job.getMemoryNeed());
			System.out.println("currentMem: " + currentMem);
			// }
		}
		if (jobQueue.peek() != null && jobQueue.peek().getMemoryNeed() > currentMem) {
			System.out.printf("Job %d with memory of %d exceeds current memory availability, remains in job queue\n\n",
					jobQueue.peek().getJobNum(), jobQueue.peek().getMemoryNeed());
		} else {
			System.out.println("Job Queue Empty");
		}
	}

	private void processSchedule() {
		// System.out.println("Inside processSchedule with arriveJobCount at " +
		// arriveJobCount);
		if (readyQueue1.isEmpty() && !readyQueue2.isEmpty() && CPU[0] == null) {
			CPU[0] = readyQueue2.remove();
			CPU[0].setFromRQ1(false);
			quantum1 = 100;
			quantum2 = 300;
			System.out.printf("Job %d was removed from RQ2 and added to CPU\n", CPU[0].getJobNum());
			System.out.printf("Job %d has Start time of %f", CPU[0].getJobNum(), CPU[0].getStartTime());
		} else if (!readyQueue1.isEmpty() && CPU[0] == null) { // remove job from readyQueue1 on put on CPU
			CPU[0] = readyQueue1.remove();
			CPU[0].setFromRQ1(true);
			quantum1 = 100;
			quantum2 = 300;
			if (CPU[0].getFromIOQueue() == false) {
				CPU[0].setStartTime(systemCounter);
				System.out.printf("Job %d added with StartTime: %f\n", CPU[0].getJobNum(), systemCounter);
			}
			System.out.printf("Job %d was removed from RQ1 and added to CPU at time: %f\n", CPU[0].getJobNum(), systemCounter);
		}

		if (CPU[0] != null && CPU[0].isFromRQ1() == false && !readyQueue1.isEmpty()) {
			readyQueue2.add(CPU[0]);
			System.out.printf("Job %d on CPU booted to RQ2, job from RQ1 added to CPU at time: %f\n", CPU[0].getJobNum(), systemCounter);
			CPU[0] = readyQueue1.remove();
			CPU[0].setFromRQ1(true);
			quantum1 = 100;
			quantum2 = 300;
			if (CPU[0].getFromIOQueue() == false) {
				CPU[0].setStartTime(systemCounter);
				System.out.printf("Job %d added with StartTime: %f\n", CPU[0].getJobNum(), systemCounter);
			}
		}
	}

	private void requestIO(int burstTimeIO) {
		requestIOCounter = 0;
		
		while (systemCounter != arriveIO) {
			System.out.println("systemCounter: " + systemCounter + " arriveIO: " + arriveIO);
			systemCounter++;
			runProcess();
		}

		System.out.printf("Event: I Time: %.0f\n", systemCounter);
		// CPU[0].setRemainingBurstTime(CPU[0].getRemainingBurstTime() + 1);
		if (CPU[0] != null) {
			CPU[0].setBurstIO(burstTimeIO); // for display
			//if(requestIOCounter > 1)
			//CPU[0].setBurstIORemain(CPU[0].getBurstIO() - 1);
			CPU[0].setIOStartTime(systemCounter);
			CPU[0].setFromIOQueue(true);
			//CPU[0].setRemainingBurstTime(CPU[0].getRemainingBurstTime() );
			System.out.printf("job %d added to IO wait queue @ time %.0f with remaining BT: %d\n", CPU[0].getJobNum(),
					systemCounter, CPU[0].getRemainingBurstTime());
			ioWaitQueue.add(CPU[0]);
			requestIOCounter++;
		}

		CPU[0] = null;
		processSchedule();
		//if(requestIOCounter == 1)
		//runProcessNoIOBurst();	
		//else
		//	runProcess();
	}

	private void completedIO() {
		//systemCounter--;
		System.out.println("Event: C Time: " + (systemCounter));
		System.out.println("job " + ioWaitQueue.peek().getJobNum() + " finished IO, added to RQ1");
		PCB jobDoneIO = ioWaitQueue.remove();
		jobDoneIO.setIOCompletion(systemCounter);
		readyQueue1.add(jobDoneIO);
		processSchedule();
	}

	private void quantumExpire() {
		PCB onCPU = CPU[0];
		systemCounter--; // SystermCounter was counting before a quantum expire could execute, had to
							// decrease to synchronize the counter
		System.out.printf("Event: E Time: %.0f Job: %d\n", systemCounter, onCPU.getJobNum());
		prevJobOnCPU = onCPU;
		readyQueue2.add(onCPU);
		System.out.println("Job " + onCPU.getJobNum() + " was added to ready queue 2");
		onCPU.setFromRQ1(false);
		CPU[0] = null;

		processSchedule();
	}

	private void jobTermination() {
		System.out.printf("Event: T Time: %.0f Job: %d\n", systemCounter, CPU[0].getJobNum());
		prevJobOnCPU = CPU[0];
		currentMem += CPU[0].getMemoryNeed();
		CPU[0].setCompletionTime(systemCounter);
		finishedQueue.add(CPU[0]);
		System.out.printf("Process %d terminated at time %.0f and added to finished queue\n", CPU[0].getJobNum(),
				systemCounter);

		System.out.println("Terminated currentMem: " + currentMem);
		CPU[0] = null;
		if (arriveJobCount > 1) {
			jobSchedule();
			processSchedule();
		}
	}

	private int stringCount(String str) {
		String trim = str.trim();
		if (trim.isEmpty())
			return 0;
		return trim.split("\\s+").length;
	}

	private String[] parseString(String command) {
		String[] parseStr = command.split("\\s+");
		return parseStr;
	}

	private void processInput(String input) {
		char eventChar = input.charAt(0);
		switch (eventChar) {
		case 'A':
			if (stringCount(input) != 5) {
				System.out.println("Invalid Number of Inputs");
				break;
			} else {
				PCB job = new PCB(parseString(input));
				arrive(job);
			}
			break;
		case 'I':
			if (stringCount(input) != 3) {
				System.out.println("Invalid Number of Inputs");
				break;
			} else {
				String[] requestIOCmd = parseString(input);
				arriveIO = Integer.parseInt(requestIOCmd[1]);
				int burstTimeIO = Integer.parseInt(requestIOCmd[2]);
				requestIO(burstTimeIO);
			}
			break;
		case 'D':
			if (stringCount(input) != 2) {
				System.out.println("Invalid Number of Inputs");
				break;
			} else {
				String[] displayCmd = parseString(input);
				double displayCount = Integer.parseInt(displayCmd[1]);
				display(displayCount);
			}
			break;
		default:
			System.out.println("Invalid event command");
		}
	}

	private void display(double displayCounter) {
		PCB job;

		while (displayCounter != systemCounter) {
			System.out.println("systemCounter: " + systemCounter + ", displayCounter: " + displayCounter);
			systemCounter++;
			runProcess();
		}

		System.out.printf("Event: D Time: %.0f\n", systemCounter);

		System.out.println("*********************************************************************");

		System.out.println("Status of the simulator at time " + displayCounter);

		// DISPLAY JOB QUEUE
		System.out.println("Contents of Job Queue");
		System.out.println("---------------------");
		if (jobQueue.size() == 0) {
			System.out.println("The Job Queue is Empty\n");
		} else {
			System.out.println("Job #  Arr.Time  Mem.Req.  Run Time");
			System.out.println("-----  --------  --------  --------");
			int jobQSize = jobQueue.size();
			while (jobQSize > 0) {
				job = jobQueue.remove();

				System.out.printf("%5d  %8d  %8d  %8d\n", job.getJobNum(), job.getArriveTime(), job.getMemoryNeed(),
						job.getBurstTime());
				jobQueue.add(job);
				jobQSize--;
			}
		}

		// DISPLAY READY QUEUE 1
		System.out.println("\nContents of First Level Ready Queue");
		System.out.println("-----------------------------------");
		if (readyQueue1.size() == 0) {
			System.out.println("The First Level Ready Queue is Empty\n");
		} else {
			System.out.println("Job #  Arr.Time  Mem.Req.  Run Time");
			System.out.println("-----  --------  --------  --------");
			int readyQ1Size = readyQueue1.size();
			while (readyQ1Size > 0) {
				job = readyQueue1.remove();

				System.out.printf("%5d  %8d  %8d  %8d\n", job.getJobNum(), job.getArriveTime(), job.getMemoryNeed(),
						job.getBurstTime());
				readyQueue1.add(job);
				readyQ1Size--;
			}
		}

		// DISPLAY READY QUEUE 2
		System.out.println("\nContents of Second Level Ready Queue");
		System.out.println("------------------------------------");
		if (readyQueue2.size() == 0) {
			System.out.println("The Second Level Ready Queue is Empty\n");
		} else {
			System.out.println("Job #  Arr.Time  Mem.Req.  Run Time");
			System.out.println("-----  --------  --------  --------");
			int readyQ2Size = readyQueue2.size();
			while (readyQ2Size > 0) {
				job = readyQueue2.remove();

				System.out.printf("%5d  %8d  %8d  %8d\n", job.getJobNum(), job.getArriveTime(), job.getMemoryNeed(),
						job.getBurstTime());
				readyQueue2.add(job);
				readyQ2Size--;
			}
		}

		// DISPLAY I/O WAIT QUEUE
		System.out.println("\nContents of I/O Wait Queue");
		System.out.println("--------------------------");
		if (ioWaitQueue.size() == 0) {
			System.out.println("The I/O Wait Queue is Empty");
		} else {
			System.out.println("Job #  Arr.Time  Mem.Req.  Run Time  IO Start Time  IO Burst  Comp. Time");
			System.out.println("-----  --------  --------  --------  -------------  --------  ----------");
			while (!ioWaitQueue.isEmpty()) {
				job = ioWaitQueue.poll();
				System.out.printf("%5d  %8d  %8d  %8d  %12.0f  %9d  %10.0f\n", job.getJobNum(), job.getArriveTime(),
						job.getMemoryNeed(), job.getBurstTime(), job.getIOStartTime(), job.getBurstIO(),
						job.getIOCompletion());
				ioWaitQueue2.add(job);
			}
			while (!ioWaitQueue2.isEmpty()) {
				ioWaitQueue.add(ioWaitQueue2.remove());
			}
		}

		// DISPLAY CPU
		System.out.println("\nThe CPU  Start Time  CPU burst time left");
		System.out.println("-------  ----------  -------------------");
		if (CPU[0] == null)
			System.out.println("CPU is Idle\n");
		else {
			job = CPU[0];
			System.out.printf("%7d  %10.0f  %19d\n", job.getJobNum(), job.getStartTime(), job.getRemainingBurstTime());
		}
		// DISPLAY FINISHED LIST
		System.out.println("\nContents of the FINISHED LIST");
		System.out.println("------------------------------");
		if (finishedQueue.size() == 0) {
			System.out.println("The Finished List is Empty\n");
		} else {
			System.out.println("Job #  Arr.Time  Mem.Req.  Run Time  Start Time  Com. Time");
			System.out.println("-----  --------  --------  --------  ----------  ---------");
			int finishedQSize = finishedQueue.size();
			while (finishedQSize > 0) {
				job = finishedQueue.remove();

				System.out.printf("%5d  %8d  %8d  %8d  %10.0f  %9.0f\n", job.getJobNum(), job.getArriveTime(),
						job.getMemoryNeed(), job.getBurstTime(), job.getStartTime(), job.getCompletionTime());
				finishedQueue.add(job);
				finishedQSize--;
			}
		}
		System.out.println("\nThere are " + currentMem + " blocks of main memory left in the system");
	}

	private void displayFinished() {
		while (CPU[0] != null) {
			systemCounter++;
			runProcess();
		}
		System.out.println("\nContents of the FINAL FINISHED LIST");
		System.out.println("------------------------------");
		if (finishedQueue.size() == 0) {
			System.out.println("The Finished List is Empty\n");
		} else {
			System.out.println("Job #  Arr.Time  Mem.Req.  Run Time  Start Time  Com. Time");
			System.out.println("-----  --------  --------  --------  ----------  ---------");
			int finishedQSize = finishedQueue.size();
			while (finishedQSize > 0) {
				PCB job = finishedQueue.remove();

				System.out.printf("%5d  %8d  %8d  %8d  %10.0f  %9.0f\n", job.getJobNum(), job.getArriveTime(),
						job.getMemoryNeed(), job.getBurstTime(), job.getStartTime(), job.getCompletionTime());
				finishedQueue.add(job);
				finishedQSize--;
			}
		}

		System.out.printf("\nThe Average Turnaround Time for the simulation was %.3f units. \n", avgTurnArndTime());
		System.out.printf("\nThe Average Job Scheduling Wait Time for the simulation was %.3f units.\n", avgJobSchWT());
		System.out.println("\nThere are " + currentMem + " blocks of main memory left in the system.");
	}

	private double avgJobSchWT() {
		PCB job;
		double totalJSWaitTime = 0;
		int finishedQSize = finishedQueue.size();

		while (finishedQSize > 0) {
			job = finishedQueue.remove();
			totalJSWaitTime += job.getJobSchWaitTime();

			finishedQueue.add(job);
			finishedQSize--;
		}

		return totalJSWaitTime / finishedQueue.size();
	}

	private double avgTurnArndTime() {
		PCB job;
		double totalTATime = 0;
		int finishedQSize = finishedQueue.size();

		while (finishedQSize > 0) {
			job = finishedQueue.remove();
			totalTATime += job.getTurnAround();

			finishedQueue.add(job);
			finishedQSize--;
		}

		return totalTATime / finishedQueue.size();
	}
}
