package com.liyf.httpclient;

import com.wordnik.swagger.annotations.ApiOperation;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
public class Tomcat {
    Logger log = Logger.getLogger(Tomcat.class);
    @Value("${ip}")
    private String ip; //读取配置文件中的参数

    @RequestMapping("/tomcat")
    @ApiOperation(value = "重启tomcat", httpMethod = "POST", response = String.class, notes = "找到指定tomcat,重启")
    public String app(HttpServletRequest request) throws IOException {
        String port = request.getParameter("port");
        String basePath = request.getParameter("basePath");
        String operation = request.getParameter("operation");
        String dir = request.getParameter("dir");
        String requestIp = HttpReq.getRemoteIP(request);
        log.info("请求的ip地址为--------------》" + ip);
        if (!ip.equals(requestIp)) {
            return "0";
        }
        log.info("operation:" + operation + "--port:" + port + "--basePath:" + basePath + "--dir:" + dir);
        String path = null;
        File f = new File(basePath + "\\");
        File[] fList = f.listFiles();
        for (File file : fList) {
            if (file.isDirectory()) {
                if (file.getName().contains(port)) {

                    if ("start".equals(operation)) {
                        path = basePath + "\\" + file.getName() + "\\bin\\startup.bat";
                        path = path.replace("\\\\", "\\");
                        log.info("Sucess---start---" + path);
                    }
                    if ("close".equals(operation)) {
                        path = basePath + "\\" + file.getName() + "\\bin\\shutdown.bat";
                        path = path.replace("\\\\", "\\");
                        log.info("Sucess---close---" + path);
                    }
                    if ("delete".equals(operation)) {
                        path = basePath + "\\" + file.getName() + "\\webapps\\" + dir;
                        //开始备份
                        LocalDate date = LocalDate.now(); // get the current date
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
                        String today=date.format(formatter);
                        String bakPath = basePath + "\\" + file.getName() + "\\bak\\"+today+"\\"+dir;
                        File bakFile = new File(bakPath);
                        if (!bakFile.exists()) {
                            bakFile.mkdirs();
                            log.info("Sucess---copy---from" + path);
                            log.info("Sucess---copy-----to" + bakPath);
                            HttpReq.copyDir(path,bakPath);
                        }

                        //开始删除
                        HttpReq.deleteDir(path);
                        log.info("Sucess to delete " + path);
                    }
                }
            }
        }
        if (path != null) {
            Runtime rt = Runtime.getRuntime(); //返回当前应用程序的Runtime对象

            Process process = null;//制子进程的执行或获取该子进程的信息
            try {
                int i = 1;

                process = rt.exec("cmd /c  " + path);
 /*               BufferedInputStream in = new BufferedInputStream(process.getInputStream());
                BufferedReader inBr = new BufferedReader(new InputStreamReader(in));
                //读取正常信息
                String lineStr;
                while ((lineStr = inBr.readLine()) != null)
                    //获得命令执行后在控制台的输出信息
                    log.info(lineStr);// 打印输出信息
                //读取错误信息
                InputStream stderr = process.getErrorStream();
                InputStreamReader isr = new InputStreamReader(stderr);
                BufferedReader br = new BufferedReader(isr);
                String line = null;
                log.info("<ERROR>");
                while ( (line = br.readLine()) != null)
                    log.info(line);
                //检查命令是否执行失败。
                if (process.waitFor() != 0) {
                    if (process.exitValue() == 1)//p.exitValue()==0表示正常结束，1：非正常结束
                        log.info("命令执行失败!");
                }
                inBr.close();
                in.close();*/

                if ("start".equals(operation)) {
                    int count = 0;
                    while (true) {
                        if (count > 10) {
                            break;
                        }
                        String surl = "http://localhost:" + port + "/httpclient4.3";
                        URL url = new URL(surl);
                        URLConnection rulConnection = url.openConnection();
                        HttpURLConnection httpUrlConnection = (HttpURLConnection) rulConnection;
                        httpUrlConnection.setConnectTimeout(10000);
                        httpUrlConnection.setReadTimeout(10000);
                        try {
                            httpUrlConnection.connect();
                            String code = new Integer(httpUrlConnection.getResponseCode()).toString();
                            String message = httpUrlConnection.getResponseMessage();
                            System.out.println("getResponseCode code =" + code);
                            System.out.println("getResponseMessage message =" + message);
                            if (code.startsWith("2")) {
                                log.info(LocalDateTime.now() + "连接httpclient4.3" + surl + "正常");
                                break;
                            }
                        } catch (Exception e) {
                            count++;
                            Thread.sleep(10000);
                            log.error(LocalDateTime.now() + "连接httpclient4.3" + surl + "异常");
                        }
                    }
                }
                if ("close".equals(operation)) {
                    String ports[] = {port};
                    TestTomcat.killPort(ports);
                }
            } catch (Exception e) {
                log.error(LocalDateTime.now() + "tomcat启动异常", e);
            }
        }
        return "1";
    }
}
