import java.util.Scanner;
import java.io.File;
import java.util.ArrayList;


public class LeaderElection {



    public static void main(String[] args) throws Exception {
       
       
      Scanner scanner = new Scanner(new File("sample-input.dat"));
     

        int n = scanner.nextInt();
        System.out.println("Number of processes: " + n);
        ArrayList < Integer > uids = new ArrayList < Integer > ();
        System.out.print("UIDs of processes: ");
        for (int i = 0; i < n; i++) {
            uids.add(scanner.nextInt());
            System.out.print(uids.get(i) + " ");
        }

        System.out.println("\n");

        Runnable processes[] = new Process[n];
        for (int i = 0; i < n; i++) {
            processes[i] = new Process(uids.get(i));

        }

        for (int i = 0; i < n; i++) {
            Process p = (Process) processes[i];
            System.out.print("Neighbors of process " + uids.get(i) + ": ");
            for (int j = 0; j < n; j++) {
                int k = scanner.nextInt();
                if (k == 1)
                {
                    System.out.print(uids.get(j) + " ");
                    p.addChannel((Process) processes[j]);
                }
            }
            System.out.print("\n");
        }
           
        scanner.close();

        // STart n threads
       Thread[] threads = new Thread[n];
        for (int i = 0; i < n; i++) {
            threads[i] = new Thread(processes[i]);
            threads[i].start();
        }

        while (true) {
            /* check if any thread is alive */
            boolean flag = false;
            /*for (Thread t: threads) {
                if (t.isAlive()) {
                    flag = true;
                    break;
                }
            }*/
            
            for(Runnable p:processes)
            {
            	Process process=(Process)p;
            	if(!process.getIsDone())
            	{
            		flag=true;
            	}
            }
            
           
            /* no thread alive, exit */
            if (!flag) {
               
                for(Runnable p:processes)
                {
                	Process process=(Process)p;
                	process.setIsTerminated(true);
                }
                break;
            }

           
           
        }
        System.out.println("Execution finished. Leader elected");
        System.out.println("Total number of messages: "+Process.getTotalMessages());

    }




}