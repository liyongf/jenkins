package com.liyf.httpclient;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


class TestTomcat {
   static Logger log = Logger.getLogger(TestTomcat.class);
    private Set<Integer> ports;//在类名下方定义一个变量


    public void callCommand(String command) throws IOException {

        Runtime runtime = Runtime.getRuntime();//返回与当前的Java应用相关的运行时对象

        //指示Java虚拟机创建一个子进程执行指定的可执行程序，并返回与该子进程对应的Process对象实例

        Process process = runtime.exec(command);

        runtime.gc();//运行垃圾回收器

        String line = null;

        String content = "";

        BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));

        while ((line = br.readLine()) != null) {
            content += line + "\r\n";
            log.info(line);
        }
    }

    //根据 端口号进行查找
    public void start(int port) {
        Runtime runtime = Runtime.getRuntime();
        try {
            //查找进程号
            Process p = runtime.exec("cmd /c netstat -ano | findstr \"" + port + "\"");
            InputStream inputStream = p.getInputStream();
            List<String> read = read(inputStream, "UTF-8");
            if (read.size() == 0) {
                log.info("找不到该端口的进程");
            } else {
                for (String string : read) {
                    log.info(string);
                }
                log.info("找到" + read.size() + "个进程，正在准备清理");
                kill(read);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //读取 java执行cmd 命令返回的 结果 只是 干掉 pid使用
    private List<String> read(InputStream in, String charset) throws IOException {
        List<String> data = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, charset));
        String line;
        while ((line = reader.readLine()) != null) {
            boolean validPort = validPort(line);
            if (validPort) {
                data.add(line);
            }
        }
        reader.close();
        return data;
    }

    //判断 端口号是否存在
    private boolean validPort(String str) {
        Pattern pattern = Pattern.compile("^ *[a-zA-Z]+ +\\S+");
        Matcher matcher = pattern.matcher(str);
        matcher.find();
        String find = matcher.group();
        int spstart = find.lastIndexOf(":");
        find = find.substring(spstart + 1);

        int port = 0;
        try {
            port = Integer.parseInt(find);
        } catch (NumberFormatException e) {
            log.info("查找到错误的端口:" + find);
            return false;
        }
        if (this.ports.contains(port)) {
            return true;
        } else {
            return false;
        }
    }

    //具体执行 干掉进程方法
    public void kill(List<String> data) {
        Set<Integer> pids = new HashSet<>();
        for (String line : data) {
            int offset = line.lastIndexOf(" ");
            String spid = line.substring(offset);
            spid = spid.replaceAll(" ", "");
            int pid = 0;
            try {
                pid = Integer.parseInt(spid);
                callCommand("taskkill /pid " + pid + " -t -f");
            } catch (NumberFormatException e) {
                log.info("获取的进程号错误:" + spid);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            pids.add(pid);
        }
    }

    public static void killPort(String[] _ports) {
        Set<Integer> ports = new HashSet<>();
        for (String spid : _ports) {
            try {
                int pid = Integer.parseInt(spid);
                ports.add(pid);
            } catch (Exception e) {
                log.info("错误的端口号，请输入一个或者多个端口，以英文逗号隔开");
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
        //machNetSetDaoImp 表示当前的类名
        TestTomcat kill = new TestTomcat();
        kill.ports = ports;
        System.out.println("need kill " + ports.size() + " num");
        for (Integer pid : ports) {
            kill.start(pid);
        }
        log.info("清理完毕，程序即将退出");
        log.info("SUCCESS");
    }

}
