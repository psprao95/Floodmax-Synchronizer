import java.util.Queue;
import java.util.ArrayList;
import java.util.concurrent.*;
public class Channel {
	
	private DelayQueue<Message> messages;
	private Process process;
	private boolean dummyFlag;
	private boolean receivedMessage;
	
	public Channel(Process process)
	{
		this.process=process;
		this.dummyFlag=true;
		messages=new DelayQueue<Message>();
	}
	
	public  void addMessage(Message message)
	{
		messages.offer(message);
	}
	
	public Process getProcess()
	{
		return process;
	}
	
	public boolean hasMessages()
	{
		return messages.size()>0;
	}
	
	
	public ArrayList<Message> getMessages()
	{
		ArrayList<Message> result=new ArrayList<Message>();
		try
		{if(!messages.isEmpty()) {
			result.add(messages.take());}
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		
		return result;
	}
	
	public void setDummyFlag(boolean b)
	{
		this.dummyFlag=b;
	}
	
	public boolean getDummyFlag()
	{
		return this.dummyFlag;
	}
	public void setgotMessage(boolean b)
	{
		this.receivedMessage=b;
	}
	
	public boolean getGotMessage()
	{
		return this.receivedMessage;
	}
	

}
