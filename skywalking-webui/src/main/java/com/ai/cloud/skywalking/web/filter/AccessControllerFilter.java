package com.ai.cloud.skywalking.web.filter;

import com.ai.cloud.skywalking.web.util.Constants;
import com.ai.cloud.skywalking.web.dto.LoginUserInfo;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by xin on 16-3-28.
 */
public class AccessControllerFilter implements Filter {


    private List<String> contained = new ArrayList<>();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        contained.add("applicationList");
        contained.add("addApplication");
        contained.add("createGlobalApplication");
        contained.add("modifyApplication");
        contained.add("showAnlySearchResult");
        contained.add("showAnalysisResult");
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        if (contained.contains(request.getParameter("loadType"))){
            LoginUserInfo loginUserInfo = (LoginUserInfo) request.getSession()
                    .getAttribute(Constants.SESSION_LOGIN_INFO_KEY);
            if (loginUserInfo == null) {
                ((HttpServletResponse) servletResponse).sendRedirect(request.getContextPath() + "/usr/login");
            }
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {

    }
}
