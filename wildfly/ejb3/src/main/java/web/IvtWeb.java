package web;

import java.io.IOException;

import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import bean.IvtBean;

// curl http://127.0.0.1:8080/ivt/call-mdb
@WebServlet("/call-mdb")
public class IvtWeb extends HttpServlet {

	@EJB
	private IvtBean ivtBean;

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		ivtBean.sendMsg();
	}
}
