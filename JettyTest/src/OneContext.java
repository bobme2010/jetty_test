
import java.io.IOException;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSessionIdListener;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;

import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;


public class OneContext extends AbstractHandler 
 {
	 
 
    public static void main(String[] args) throws Exception {
        Server server = new Server(9100);
 
//        ContextHandler context = new ContextHandler();     
//        context.setContextPath("/servlet/");
//        context.setResourceBase(".");
//        context.setClassLoader(Thread.currentThread().getContextClassLoader());
        
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);  
		context.setClassLoader(Thread.currentThread().getContextClassLoader()); 
		context.setContextPath("/");
		
        // http://localhost:8080/hello  
        context.setHandler(new OneContext());
        
        server.setHandler(context);  

               
        
        server.start();
        server.join();
    }

    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);
        Date a = new  Date();
        response.getWriter().println("<h1>"+ a +"</h1>");
//        if (_body!=null)
//            response.getWriter().println(_body);
    }

}
