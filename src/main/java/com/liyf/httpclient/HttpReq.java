package com.liyf.httpclient;

import com.wordnik.swagger.annotations.ApiOperation;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
public class HttpReq {
    static Logger log = Logger.getLogger(HttpReq.class);
    @Value("${ip}")
    private String ip; //读取配置文件中的参数

    @RequestMapping("/app")
    @ApiOperation(value = "备份文件，重启tomcat", httpMethod = "POST", response = String.class, notes = "找到指定tomcat，备份upload和uploadTemp文件，重启tomcat")
    public String app(HttpServletRequest request) throws UnsupportedEncodingException {
        request.setCharacterEncoding("utf-8");
        String direction = request.getParameter("direction");
        String port = request.getParameter("port");
        String basePath = request.getParameter("basePath");
        String requestIp = getRemoteIP(request);
        log.info("请求的ip地址为--------------》" + ip);
        if (!ip.equals(requestIp)) {
            return "0";
        }
        log.info("direction:" + direction + "--port:" + port + "--basePath:" + basePath);
        try {
            File f = new File(basePath + "\\");
            File[] fList = f.listFiles();
            for (File file : fList) {
                if (file.isDirectory()) {
                    if (file.getName().contains(port)) {
                        String path = basePath + "\\" + file.getName() + "\\webapps";
                        if ("out".equals(direction)) {
                            String fromUploadPath = getFromUploadDir(path, "upload");
                            String fromUploadTempPath = getFromUploadDir(path, "uploadTemp");
                            if (fromUploadPath != null) {
                                String toUploadPath = basePath + "\\" + file.getName() + "\\bak" + "\\upload";
                                moveDir(direction, fromUploadPath, toUploadPath);
                            }
                            if (fromUploadTempPath != null) {
                                String toUploadTempPath = basePath + "\\" + file.getName() + "\\bak" + "\\uploadTemp";
                                moveDir(direction, fromUploadTempPath, toUploadTempPath);
                            }
                        }
                        if ("in".equals(direction)) {
                            String toPath = basePath + "\\" + file.getName() + "\\bak";
                            String toUploadPath = getToUploadDir(toPath, "upload");
                            String toUploadTempPath = getToUploadDir(toPath, "uploadTemp");
                            if (toUploadPath != null) {
                                String fromUploadPath = getFromUploadDir(path, "upload");
                                moveDir(direction, fromUploadPath, toUploadPath);
                            }
                            if (toUploadTempPath != null) {
                                String fromUploadTempPath = getUploadDir(path, "upload");
                                moveDir(direction, fromUploadTempPath, toUploadTempPath);
                            }
                        }

                    }
                }
            }
        } catch (IOException e) {
            log.error("备份文件异常", e);
        }
        return "1";
    }


    public static void moveDir(String direction, String fromDirPath, String toDir) throws IOException {
        log.info("备份文件方向--->" + direction + "form------>" + fromDirPath + "to------->" + toDir);
        if ("out".equals(direction)) {
            File file = new File(toDir);
            if (!file.exists()) {
                file.mkdirs();
            } else {
                deleteDir(toDir);
            }
            copyDir(fromDirPath, toDir);
        }
        if ("in".equals(direction)) {
            File file = new File(fromDirPath);
            if (!file.exists()) {
                file.mkdirs();
            } else {
                deleteDir(fromDirPath);
            }
            copyDir(toDir, fromDirPath);
        }
    }


    /**
     * 复制目录
     *
     * @param fromDir
     * @param toDir
     * @throws IOException
     */
    public static void copyDir(String fromDir, String toDir) throws IOException {
        //创建目录的File对象
        File dirSouce = new File(fromDir);
        //判断源目录是不是一个目录
        if (!dirSouce.isDirectory()) {
            //如果不是目录那就不复制
            return;
        }
        //创建目标目录的File对象
        File destDir = new File(toDir);
        //如果目的目录不存在
        if (!destDir.exists()) {
            //创建目的目录
            destDir.mkdir();
        }
        //获取源目录下的File对象列表
        File[] files = dirSouce.listFiles();
        for (File file : files) {
            //拼接新的fromDir(fromFile)和toDir(toFile)的路径
            String strFrom = fromDir + File.separator + file.getName();
            //System.out.println(strFrom);
            String strTo = toDir + File.separator + file.getName();
            //System.out.println(strTo);
            //判断File对象是目录还是文件
            //判断是否是目录
            if (file.isDirectory()) {
                //递归调用复制目录的方法
                copyDir(strFrom, strTo);
            }
            //判断是否是文件
            if (file.isFile()) {
                //System.out.println("正在复制文件：" + file.getName());
                //递归调用复制文件的方法
                copyFile(strFrom, strTo);
            }
        }
    }

    /**
     * 复制文件
     *
     * @param fromFile
     * @param toFile
     * @throws IOException
     */
    public static void copyFile(String fromFile, String toFile) throws IOException {
        //字节输入流——读取文件
        FileInputStream in = new FileInputStream(fromFile);
        //字节输出流——写入文件
        FileOutputStream out = new FileOutputStream(toFile);
        //把读取到的内容写入新文件
        //把字节数组设置大一些   1*1024*1024=1M
        byte[] bs = new byte[1 * 1024 * 1024];
        int count = 0;
        while ((count = in.read(bs)) != -1) {
            out.write(bs, 0, count);
        }
        //关闭流
        in.close();
        out.flush();
        out.close();
    }

    public static String getFromUploadDir(String strPath, String upload) {
        List<File> list = new ArrayList<File>();
        File f = new File(strPath);
        File[] fList = f.listFiles();

        for (int j = 0; j < fList.length; j++) {
            if (fList[j].isDirectory()) {
                File[] fList1 = fList[j].listFiles();
                list.addAll(Arrays.asList(fList1));
            }
        }
        for (File ff : list) {
            if (upload.equals(ff.getName())) {
                return ff.getPath();
            }
        }
        return null;
    }

    public static String getToUploadDir(String strPath, String upload) {
        List<File> list = new ArrayList<File>();
        File f = new File(strPath);
        File[] fList = f.listFiles();

        for (int j = 0; j < fList.length; j++) {
            if (fList[j].isDirectory()) {
                list.add(fList[j]);
            }
        }
        for (File ff : list) {
            if (upload.equals(ff.getName())) {
                return ff.getPath();
            }
        }
        return null;
    }

    public static String getUploadDir(String strPath, String upload) {

        File f = new File(strPath);
        File[] fList = f.listFiles();

        for (int j = 0; j < fList.length; j++) {
            if (fList[j].isDirectory()) {
                File[] fList1 = fList[j].listFiles();
                List<File> list = new ArrayList<File>();
                list.addAll(Arrays.asList(fList1));
                for (File ff : list) {
                    if (upload.equals(ff.getName())) {
                        return fList[j].getPath() + "\\uploadTemp";
                    }
                }
            }
        }

        return null;
    }

    public static boolean deleteDir(String path) {
        File file = new File(path);
        if (!file.exists()) {//判断是否待删除目录是否存在
            log.error("The dir are not exists!" + path);
            return false;
        }

        String[] content = file.list();//取得当前目录下所有文件和文件夹
        for (String name : content) {
            File temp = new File(path, name);
            if (temp.isDirectory()) {//判断是否是目录
                deleteDir(temp.getAbsolutePath());//递归调用，删除目录里的内容
                temp.delete();//删除空目录
            } else {
                if (!temp.delete()) {//直接删除文件
                    log.error("Failed to delete " + name);
                }
            }
        }
        file.delete();
        return true;
    }

    public static String getRemoteIP(HttpServletRequest request) {
        String ip = null;
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Forwarded-For");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null) {
            //对于通过多个代理的情况，最后IP为客户端真实IP,多个IP按照','分割
            int position = ip.indexOf(",");
            if (position > 0) {
                ip = ip.substring(0, position);
            }
        }
        return ip;
    }
}
