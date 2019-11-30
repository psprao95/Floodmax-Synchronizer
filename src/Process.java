import java.util.ArrayList;

public class Process implements Runnable {
    private int uid;
    private int max_so_far;

    private Status status;
    private int round;
    private Process parent;
    private int grandParent;


    ArrayList < Channel > channels;
    ArrayList < Message > inbox;
    ArrayList < Message > rem;


    private int requiredAcks;
    private boolean allAcksReturned;
    private boolean newInfo;
    private boolean leader_found;
    
    private volatile boolean isDone;
    private volatile boolean isTerminated;

    private static int totalMessages = 0;


    public Process(int uid) {
        this.uid = uid;
        this.status = Status.UNKNOWN;
        this.round = 1;
        this.channels = new ArrayList < Channel > ();
        this.rem = new ArrayList < Message > ();
        inbox = new ArrayList < Message > ();
        this.leader_found = false;

        this.newInfo = true;
        this.parent = null;
        this.grandParent = -1;
        this.max_so_far = uid;
        this.isDone=false;
        this.isTerminated=false;


    }

    public int getId() {
        return uid;
    }

    public boolean getIsDone()
    {
    	return this.isDone;
    }
    
    public void setIsTerminated(boolean b)
    {
    	this.isTerminated=b;
    }
    
    	public void addChannel(Process neighbor) {
    
        channels.add(new Channel(neighbor));
    }

    public void flood(Message message) {
        for (Channel channel: channels) {
            if (!channel.getProcess().equals(this.parent)) {
                transmitMessage(channel.getProcess(), message);
            }
        }
    }

    public static int getTotalMessages()

    {
        return totalMessages;
    }

    public void transmitMessage(Process receiver, Message message) {
        message.generateTime();


        System.out.println("Round: " + this.round + " Message : " + message.getType() + " Content: " + message.getId() +
            " From: Process " + this.uid + " To: Process " + receiver.getId());
        Channel channel = getChannel(receiver.getId());


        receiver.putMessage(message);
        if(!message.getType().equals(MessageType.DUMMY))
        {
        	totalMessages++;
        }

        channel.setDummyFlag(false);


    }

    public void putMessage(Message message) {
        int sender = message.getSender();
        Channel channel = getChannel(sender);
        if (this.getChannel(sender) != null) {
            channel.addMessage(message);
        }
    }

    public Channel getChannel(int id) {
        for (Channel channel: channels) {
            if (channel.getProcess().getId() == id) {
                return channel;
            }
        }
        return null;
    }

    public void initPhase() {

        Message message = new Message(max_so_far, uid, -1, MessageType.EXPLORE);
        this.allAcksReturned = false;
        if (this.parent != null) {
            this.requiredAcks = channels.size() - 1;
        } else {
            this.requiredAcks = channels.size();
        }
        flood(message);
    }

    public void sendDummys() {
        for (Channel channel: channels) {
            if (channel.getDummyFlag() == true) {
                Message message = new Message(0, 0, 0, MessageType.DUMMY);
                transmitMessage(channel.getProcess(), message);

            }
        }
    }

    public void goTonextRound() {
        this.round++;

        this.newInfo = false;
        sendDummys();
        for (Channel channel: channels) {
            Process neighbor = channel.getProcess();
            Channel nbrChannel = neighbor.getChannel(this.uid);
            if (!nbrChannel.hasMessages()) {
                channel.setDummyFlag(true);
            }

            channel.setgotMessage(false);
        }
        inbox = new ArrayList < Message > ();

    }



    @Override
    public void run() {
        try {
        while (!this.isTerminated) {

            if (round == 1) {
                initPhase();
                goTonextRound();
                continue;
            }
            
            if(leader_found)
            {
            	Thread.sleep(10);
            	goTonextRound();
            }
            
            /*receive one message from each channel */
            while (true) {
                boolean flag = false;
                for (Channel channel: channels) {
                    if (channel.getGotMessage() == false) {
                        flag = true;
                        rem = channel.getMessage();
                        if (!rem.isEmpty()) {
                            channel.setgotMessage(true);
                        }
                        inbox.addAll(rem);
                    }
                }
                if (!flag) {
                    break;
                }
            }

            /* process the received messages */
            for (Message message: inbox) {
                if (message.getType().equals(MessageType.LEADER_BROADCAST) && this.status.equals(Status.UNKNOWN)) {
                    this.status = Status.NON_LEADER;
                    Message m = new Message(message.getId(), this.uid, -1, MessageType.LEADER_BROADCAST);
                    flood(m);
                    leader_found = true;
                    System.out.println("Process " + this.uid + ": Process " + message.getId() + " is the leader");
                    break;
                }
            }

            if (!leader_found) {

                for (Message message: inbox) {
                    if (message.getType().equals(MessageType.EXPLORE) && message.getId() > this.max_so_far) {
                        newInfo = true;
                        this.max_so_far = message.getId();
                        this.allAcksReturned = false;
                        this.parent = getChannel(message.getSender()).getProcess();
                        this.grandParent = message.getSenderParent();
                    }
                }

                if (newInfo) {
                    int p;
                    if (parent == null) {
                        p = -1;
                    } else {
                        p = parent.getId();
                    }
                    Message message = new Message(max_so_far, uid, p, MessageType.EXPLORE);

                    if (this.parent != null) {
                        this.requiredAcks = channels.size() - 1;
                    } else {
                        this.requiredAcks = channels.size();
                    }
                    flood(message);
                }

                for (Message m: inbox) {
                    if (m.getType().equals(MessageType.EXPLORE) && m.getId() <= max_so_far) {
                        int sender = m.getSender();
                        Channel channel = getChannel(sender);
                        Process neighbor = channel.getProcess();
                        Message reject = new Message(this.max_so_far, this.uid, m.getSenderParent(), MessageType.REJECT);
                        transmitMessage(neighbor, reject);
                    }

                    if (m.getType().equals(MessageType.REJECT) || m.getType().equals(MessageType.COMPLETED)) {
                        if (this.parent != null && this.parent.getId() == m.getSenderParent()) {
                            this.requiredAcks--;
                        } else if (parent == null && m.getSenderParent() == -1) {
                            this.requiredAcks--;
                        }

                        if (!allAcksReturned && requiredAcks == 0 && this.status.equals(Status.UNKNOWN) && parent != null) {
                            allAcksReturned = true;
                            Message accept = new Message(this.max_so_far, this.uid, this.grandParent, MessageType.COMPLETED);
                            transmitMessage(this.parent, accept);
                        }
                    }
                }

                if (requiredAcks == 0 && this.parent == null && this.status.equals(Status.UNKNOWN)) {
                    this.status = Status.LEADER;
                    this.leader_found = true;
                    Message leader_broadcast = new Message(this.uid, this.uid, -1, MessageType.LEADER_BROADCAST);
                    System.out.println("Process " + this.uid + ": I am leader");
                    flood(leader_broadcast);
                }

            }

            if (leader_found) {

                
                
                this.isDone=true;
                //break;

            }
            goTonextRound();


        }
        System.out.println("Process " + this.uid + ": I am terminating ...");
        }
        catch(Exception e)
        {
        	e.printStackTrace();
        }

    }




}