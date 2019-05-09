package project;

import java.util.Comparator;

public class IOBurstComparator implements Comparator<PCB> {

	@Override
	public int compare(PCB process1, PCB process2) {
		// TODO Auto-generated method stub
		return process1.getBurstIORemain() - process2.getBurstIORemain();
	}
}
