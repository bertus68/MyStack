package a.polverini.my;

import android.app.*;
import android.os.*;
import java.util.*;
import a.polverini.my.MainActivity.Stack.*;
import android.content.*;
import android.widget.*;
import android.webkit.*;
import java.io.*;
import android.view.*;
import a.polverini.my.MainActivity.*;
import android.graphics.*;
import android.graphics.drawable.*;

public class MainActivity extends Activity 
{
	private boolean verbose = false;
	private HtmlHandler handler;
	private ProgressBar progress; 
	private WebView webView;
	private Menu menu;
	
	private Stack stack;
	
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);
		new AsyncTask() {

			@Override
			protected Object doInBackground(Object[] args)
			{
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {}
				return args[0];
			}

			@Override
			public void onPostExecute(Object result) {
				setContentView(result);

				progress = findViewById(R.id.PROGRESS);

				webView = findViewById(R.id.WEBVIEW);
				webView.getSettings().setJavaScriptEnabled(true);
				webView.getSettings().setBuiltInZoomControls(true);
				webView.setWebViewClient(new MyWebViewClient());
				
				handler = new HtmlHandler(webView);
				print("<h1>MyStack v0.1.0</h1>");
				print("A.Polverini (2018)<p/>");
				
				
				//test();
			}

		}.execute(R.layout.main);
		
		stack = new Stack();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		this.menu = menu;
		return true;
	}
	
	boolean play;
	boolean pause;
	boolean stop;

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		int color = 0xFFFF0000;
		final PorterDuffColorFilter filter = new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY);
		
		try {
			switch (item.getItemId()) {
				case R.id.STATUS:
					break;
				case R.id.PLAY:
					play = !play;
					command("color "+R.id.PLAY+" "+(play?0xFFFF0000:0xFF000000));
					break;
				case R.id.PAUSE:
					pause = !pause;
					command("color "+R.id.PAUSE+" "+(pause?0xFF00FF00:0xFF000000));
					break;
				case R.id.STOP:
					stop = !stop;
					command("color "+R.id.STOP+" "+(stop?0xFF0000FF:0xFF000000));
					break;
				default:
					return super.onOptionsItemSelected(item);
			}
		} catch(Exception e) {
			print(e);
		}
		return true;
	}
	
	void test() {
		
		StackControlServiceFactory factory = (StackControlServiceFactory)services.get("StackControlServiceFactory");
		
		StackControlService control = factory.getService(new IStackControlListener() {
				
				@Override
				public void statusChanged(StackStatus status)
				{
					println("status = %s", status);
					command("status "+status.toString());
				}

				@Override
				public void confirmationRequest(StackEntry entry)
				{
					// TODO: Implement this method
				}
				
			});
		try {
			try {
				println("addRequest while IDLE (expected to be rejected)...");
				control.addRequest(new Request("X"), 0);
			} catch (Exception e) {
				print(e);
			} finally {
				if(verbose) dump();
			}
			
			control.subscribe();
			
			try {
				println("addRequest \"A\", \"B\", \"C\" and \"D\" while EDITING (expected to be accepted)...");
				control.addRequest(new Request("A"), 0);
				control.addRequest(new Request("B"), 0);
				control.addRequest(new Request("C"), 0);
				control.addRequest(new Request("D"), 0);
			} catch (Exception e) {
				print(e);
			} finally {
				if(verbose) dump();
			}
			
			try {
				println("setDispatchTime 1 +3s...");
				control.setDispatchTime(1, new RelativeTime(3000));
			} catch (Exception e) {
				print(e);
			} finally {
				if(verbose) dump();
			}
			
			try {
				println("addRequest \"E\" in given position (1)...");
				control.addRequest(new Request("E"), 1);
				println("setDispatchTime 1 +5s...");
				control.setDispatchTime(1, new RelativeTime(5000));
			} catch (Exception e) {
				print(e);
			} finally {
				if(verbose) dump();
			}
			
			try {
				println("moveRequests \"E\" to the end of the stack...");
				control.moveRequests(1,1,0);
			} catch (Exception e) {
				print(e);
			} finally {
				if(verbose) dump();
			}
			
			stack.start();
			
		} catch (Exception e) {
			print(e);
		}
		
    }

	private void dump()
	{
		println("%d entries:", stack.size());
		StackEntry[] entries = stack.getEntries();
		for (int i=0;i < stack.size();i++) {
			println("%d = %s", i + 1, entries[i]);
		}
	}

	class StackUI {
		
	}
	
	class Request
	{
		private String name;
		
		public Request(final Request request) {
			this(request.name);
		}
		
		public Request(String name) {
			this.name = name;
		}
		
		public final String toString() {
			return name;
		}
		
	}
	
	abstract class Time {

	}
	
	class AbsoluteTime extends Time {
		
	}
	
	class RelativeTime extends Time {
		
		private long ms = 0;
		
		public void set(long ms) {
			this.ms = ms;
		}
		
		public long get() {
			return this.ms;
		}
		
		public RelativeTime(long ms) {
			set(ms);
		}
		
		public String toString() {
			return String.format(" after %5.3f seconds",(ms/1000.0f));
		}
		
	}
	
	abstract class ServiceFactory {
		
	}
	
	Map<String, ServiceFactory> services = new HashMap<>();
	
	interface IStackListener {
		public void statusChanged(StackStatus status);
	}
	
	interface IStackEditor {
		public void subscribe() throws Exception;
		public void addRequest(Request request, int position) throws NotAllowedException, IllegalArgument;
		public void deleteRequests(int start, int end) throws NotAllowedException, IllegalArgument;
		public void copyRequests(int start, int end, int position) throws NotAllowedException, IllegalArgument;
		public void moveRequests(int start, int end, int position) throws NotAllowedException, IllegalArgument;
		public void setDispatchTime(int position, Time time) throws NotAllowedException, IllegalArgument;
		public void unsubscribe();
	}
	
	interface IStackControl extends IStackEditor {
		public void subscribe() throws Exception;
		public void unsubscribe();
		public void start();
		public void stop();
		public void pause();
		public void resume();
		public void arm();
		public void disarm();
		public void go();
	}
	
	interface IStackControlListener extends IStackListener {
		public void confirmationRequest(StackEntry entry);
	}
	
	interface IStackMonitor {
		public void subscribe() throws Exception;
		public void unsubscribe();
	}
	
	class StackEditorService implements IStackEditor
	{

		private static final String TAG = "StackEditorService";
		
		private IStackListener listener;

		public StackEditorService(IStackListener listener) {
			this.listener = listener;
		}
		
		@Override
		public void subscribe() throws Exception {
			if(verbose) println(TAG+".subscribe()");
			stack.addListener(listener);
			stack.setStatus(StackStatus.EDITING);
		}

		@Override
		public void unsubscribe() {
			if(verbose) println(TAG+".unsubscribe()");
			stack.removeListener(listener);
		}
		
		@Override
		public void addRequest(Request request, int position) throws NotAllowedException, IllegalArgument {
			if(verbose) println(TAG+".addRequest()");
			stack.addRequest(request, position);
		}
		
		@Override
		public void deleteRequests(int start, int end) throws NotAllowedException, IllegalArgument {
			if(verbose) println(TAG+".addRequest()");
			stack.deleteRequests(start, end) ;
		}
		
		@Override
		public void copyRequests(int start, int end, int position) throws NotAllowedException, IllegalArgument {
			if(verbose) println(TAG+".addRequest()");
			stack.copyRequests(start, end, position);
		}
		
		@Override
		public void moveRequests(int start, int end, int position) throws NotAllowedException, IllegalArgument {
			if(verbose) println(TAG+".addRequest()");
			stack.moveRequests(start, end, position);
		}

		@Override
		public void setDispatchTime(int position, Time dispatchTime) throws NotAllowedException, IllegalArgument {
			if(verbose) println(TAG+".addRequest()");
			stack.setDispatchTime(position, dispatchTime);
		}
		
	}
	
	class StackControlService extends StackEditorService implements IStackControl
	{

		private static final String TAG = "StackControlService";
		
		private IStackControlListener listener;
		
		public StackControlService(IStackControlListener listener) {
			super(listener);
			this.listener = listener;
		}
		
		@Override
		public void subscribe() throws Exception {
			if(verbose) println(TAG+".subscribe()");
			super.subscribe();
			stack.addListener(listener);
		}

		@Override
		public void unsubscribe() {
			if(verbose) println(TAG+".unsubscribe()");
			super.unsubscribe();
			stack.removeListener(listener);
		}
		
		@Override
		public void start() {
			stack.start();
		}
		
		@Override
		public void stop() {
			stack.stop();
		}

		@Override
		public void pause()
		{
			// TODO: Implement this method
		}

		@Override
		public void resume()
		{
			// TODO: Implement this method
		}

		@Override
		public void arm()
		{
			// TODO: Implement this method
		}

		@Override
		public void disarm()
		{
			// TODO: Implement this method
		}

		@Override
		public void go()
		{
			// TODO: Implement this method
		}
		
	}
	
	class StackMonitorService implements IStackMonitor
	{
		private static final String TAG = "StackMonitorService";
		
		private IStackListener listener;
		
		public StackMonitorService(IStackListener listener) {
			this.listener = listener;
		}
		
		@Override
		public void subscribe() throws Exception {
			if(verbose) println(TAG+".subscribe()");
			stack.addListener(listener);
		}

		@Override
		public void unsubscribe() {
			if(verbose) println(TAG+".unsubscribe()");
			stack.removeListener(listener);
		}

	}
	
	class StackEditorServiceFactory extends ServiceFactory {
		StackEditorService getService(IStackListener listener) {
			return new StackEditorService(listener);
		}
	}
	
	class StackControlServiceFactory extends ServiceFactory {
		StackControlService getService(IStackControlListener listener) {
			return new StackControlService(listener);
		}
	}
	
	class StackMonitorServiceFactory extends ServiceFactory {
		StackMonitorService getService(IStackListener listener) {
			return new StackMonitorService(listener);
		}
	}
	
	class Stack implements Runnable {
		
		private static final String TAG = "Stack";
		
		private StackEditorServiceFactory stackEditorServiceFactory = new StackEditorServiceFactory();
		
		private StackControlServiceFactory stackControlServiceFactory = new StackControlServiceFactory();
		
		private StackMonitorServiceFactory stackMonitorServiceFactory = new StackMonitorServiceFactory();
		
		enum StackStatus {
			IDLE,
			EDITING,
			READY,
			ARMED,
			DISPATCHING,
			PAUSED
		} 
		
		private StackStatus status = StackStatus.IDLE;
		
		public void setStatus(StackStatus status ) {
			if(verbose) println(TAG+".setStatus(status=%s)",status.toString());
			this.status = status;
			for(IStackListener listener : listeners) {
				listener.statusChanged(status);
			}
		}
		
		private Vector<IStackListener> listeners = new Vector<>();

		void addListener(IStackListener listener) {
			if(verbose) println(TAG+".addListener(listener)");
			if(!listeners.contains(listener)) {
				listeners.add(listener);
			}
		}

		void removeListener(IStackListener listener) {
			if(verbose) println(TAG+".removeListener(listener)");
			if(listeners.contains(listener)) {
				listeners.remove(listener);
			}
		}
		
		public Stack() {
			services.put("StackEditorServiceFactory", stackEditorServiceFactory);
			services.put("StackControlServiceFactory", stackControlServiceFactory);
			services.put("StackMonitorServiceFactory", stackMonitorServiceFactory);
		}
		
		public StackStatus getStatus() {
			return this.status;
		}
		
		class StackEntry {
			
			private Request request;
			
			public StackEntry(final StackEntry entry) {
				this(new Request(entry.request));
				this.dispatchTime = entry.dispatchTime;
			}
			
			public StackEntry(Request request) {
				this.request = request;
			}
			
			public Request getRequest() {
				return this.request;
			}
			
			public final String toString() {
				return request.toString() + (dispatchTime==null?"":dispatchTime.toString());
			}
			
			private Time dispatchTime = null;
			
			public void setDispatchTime(Time dispatchTime) {
				this.dispatchTime = dispatchTime;
			}
			
			public Time getDispatchTime() {
				return dispatchTime;
			}
			
		}
		
		
		private Vector<StackEntry> entries = new Vector<>();
		
		public StackEntry[] getEntries() {
			return entries.toArray(new StackEntry[entries.size()]);
		}
		
		public int size() {
			return entries.size();
		}
		
		class NotAllowedException extends Error {
			public NotAllowedException(String operation, StackStatus status) {
				super("operation "+operation+" not allowed if "+status);
			}
		}
		
		class IllegalArgument extends Error {
			public IllegalArgument(String operation, String... arguments) {
				super("illegal argument "+String.join(", ", arguments)+" for "+operation);
			}
		}
		
		public void addRequest(Request request, int position) throws NotAllowedException, IllegalArgument {
			if(status!=StackStatus.EDITING) throw new NotAllowedException("addRequest", status);
			if(position < 0 || position > entries.size()) throw new IllegalArgument("addRequest", "position="+position);
			StackEntry entry = new StackEntry(request);
			if (position == 0) {
				entries.add(entry);
			} else {
				entries.insertElementAt(entry, position - 1);
			}
		}
		
		public void deleteRequests(int start, int end) throws NotAllowedException, IllegalArgument {
			if(status!=StackStatus.EDITING) throw new NotAllowedException("addRequest", status);
			if(start<1||start>entries.size()) throw new IllegalArgument("deleteRequests", "start="+start);
			if(end<0||end>entries.size()) throw new IllegalArgument("deleteRequests", "end="+end);
			if(end!=0 && start>end) throw new IllegalArgument("deleteRequests", "start="+start, "end="+end);
			if(end==0) {
				end = entries.size();
			}
			for(int i=end; i>=start; i--) {
				entries.remove(i-1);
			}
		}
		
		public void copyRequests(int start, int end, int position) throws NotAllowedException, IllegalArgument {
			if(status!=StackStatus.EDITING) throw new NotAllowedException("addRequest", status);
			if(position<0||position>entries.size()) throw new IllegalArgument("deleteRequests", "position="+position);
			if(position>=start && position<=end) throw new IllegalArgument("deleteRequests", "start="+start, "end="+end, "position="+position);
			if(start<1||start>entries.size()) throw new IllegalArgument("deleteRequests", "start="+start);
			if(end<0||end>entries.size()) throw new IllegalArgument("deleteRequests", "end="+end);
			if(end!=0 && start>end) throw new IllegalArgument("deleteRequests", "start="+start, "end="+end);
			if(end==0) {
				end = entries.size();
			}
			if(position==0) {
				position = entries.size();
			}
			List<StackEntry> clipboard = new ArrayList<>();
			for(int i=end; i>=start; i--) {
				clipboard.add(new StackEntry(entries.get(i-1)));
				
			}
			Iterator<StackEntry> iterator = clipboard.iterator();
			while(iterator.hasNext()) {
				entries.insertElementAt(iterator.next(), position);
			}
		}
		
		public void moveRequests(int start, int end, int position) throws NotAllowedException, IllegalArgument {
			if(status!=StackStatus.EDITING) throw new NotAllowedException("addRequest", status);
			copyRequests(start, end, position);
			if(position==0 || position>end) {
				deleteRequests(start, end);
			} else {
				deleteRequests(start+position, end+position);
			}
		}

		public void setDispatchTime(int position, Time dispatchTime) throws MainActivity.Stack.NotAllowedException, MainActivity.Stack.IllegalArgument {
			if(status!=StackStatus.EDITING) throw new NotAllowedException("addRequest", status);
			if(position<1 || position>entries.size()) {
				throw new IllegalArgument("setDispatchTime", "position="+position);
			}
			entries.elementAt(position-1).setDispatchTime(dispatchTime);
		}
		
		private boolean running = false;
		private Thread runner = null;
		private Thread holder = null;
		
		@Override
		public void run() {
			running = true;
			try {
				setStatus(StackStatus.DISPATCHING);
				int index = 1;
				while(running) {
					if(index>entries.size()) break;
					StackEntry entry = entries.get(index - 1);
					
					Time dispatchTime = entry.getDispatchTime();
					if(dispatchTime!=null) {
						if(dispatchTime instanceof RelativeTime) {
							println("waiting...");
							Thread.sleep(((RelativeTime)dispatchTime).get());
						}
					}
					/*
					if(status==StackStatus.PAUSED) {
						if(holder!=null) {
							try {
								holder.join();
							} catch(InterruptedException e) {
								// nothing
							}
						}
					}
					*/
					println("dispatching...%s",entry.getRequest().toString());
					index++;
				}
			//} catch(InterruptedException e) {
			//	print(e);
			} catch(Exception e) {
				print(e);
			} finally {
				setStatus(StackStatus.EDITING);
			}
			
			running = false;
		}
		
		public void start() {
			if(runner==null) {
				runner = new Thread(this);
				runner.start();
			}
		}
		
		public void stop() {
			if(runner!=null) {
				try {
					running = false;
					runner.join(1000, 0);
					if (runner.isAlive()) {
						runner.interrupt();
					}
				} catch (InterruptedException e) {
					// nothing
				}
				runner = null;
			}
		}
		
		public void pause() {
			if(holder==null) {
				holder = new Thread() {
					@Override
					public void run() {
						try {
							join();
						} catch (InterruptedException e) {

						}
					}
				};
				runner.start();
			}
		}

		public void resume() {
			if(holder!=null) {
				holder.interrupt();
				holder = null;
			}
		}
	}

	public class MyWebAppInterface {

		Context context;

		public MyWebAppInterface(Context c) {
			context = c;
		}

		@JavascriptInterface
		public void showToast(String toast) {
			Toast.makeText(context, toast, Toast.LENGTH_SHORT).show();
		}

	}

	public class MyWebViewClient extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			view.loadUrl(url);
			return true;
		}
	}

	public class HtmlHandler extends Handler {

		private WebView view;
		private StringBuffer buffer = new StringBuffer();

		public HtmlHandler(WebView view) {
			super(Looper.getMainLooper());
			this.view = view;
		}

		@Override
		public void handleMessage(Message message) {
			switch(message.what) {
				case 0:
					if(message.obj instanceof String) {
						String[] args = ((String)message.obj).split(" ");
						switch (args[0]) {
							case "clear":
								if(buffer.length()>0) {
									buffer.delete(0,buffer.length());
									view.loadData(buffer.toString(), "text/html", "UTF-8");
								}
								break;
							case "load":
								if(args.length>1) {
									view.loadUrl(args[1]);
								}
								break;
							case "status":
								if(args.length>1) {
									MenuItem item = menu.findItem(R.id.STATUS);
									item.setTitle(args[1]);
								}
								break;
							case "color":
								if(args.length>2) {
									int id = Integer.parseInt(args[1]);
									int color = Integer.parseInt(args[2]);
									try {
										MenuItem item = menu.findItem(id);
										item.getIcon().setTint(color);
									} catch(Exception e) {
										print(e);
									}
								}
								break;
							default:
								break;
						}
					}
					break;
				case 1:
					if(message.obj instanceof String) {
						buffer.append((String)message.obj);
						view.loadData(buffer.toString(), "text/html", "UTF-8");
					}
					break;
				default:
					break;
			}
		}
	}

	public class Warning extends Exception {
		public Warning(String fmt, Object... args) {
			super(String.format(fmt,args));
		}
	}

	public class Error extends Exception {
		public Error(String fmt, Object... args) {
			super(String.format(fmt,args));
		}
	}

	public void command(String s) {
		handler.obtainMessage(0, s).sendToTarget();
	}

	public void print(String fmt, Object... args) {
		String s = String.format(fmt, args);
		handler.obtainMessage(1, s).sendToTarget();
	}

	public void println(String fmt, Object... args) {
		print(fmt+"<br>\n", args);
	}

	public void println() {
		println("");
	}

	public void print(Exception e, String fmt, Object... args) {
		String s = String.format(fmt, args);
		String color = (e instanceof Warning) ? "orange" : "red";
		print("<p style='color:%s'>%s %s %s</p>", color, e.getClass().getSimpleName(), e.getMessage(), s);
		if(verbose) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			pw.close();
			print("<pre style='color:%s'>%s</pre>", color, sw.getBuffer().toString());
		}
	}

	public void print(Exception e) {
		print(e, "");
	}
}
