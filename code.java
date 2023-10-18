package com.mwxz.service.impl;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MwxzServiceImpl2 {

    public static void main(String[] args) {

        AtomicInteger count= new AtomicInteger();
        //页数
        List<Integer> list = new ArrayList<>();

        //down集合
        Set<String> down = new CopyOnWriteArraySet<>();

        //漫画名字
        String name = "幸福小岛";

        //基本封包
        Unirest.config().proxy("127.0.0.1", 7890);
        HttpResponse<String> home = Unirest.get("https://www.wnacg.com/albums-index-tag-" + name + ".html").asString();

        //匹配页数
        Matcher matcherPage = Pattern.compile("albums-index-page-(\\d)-tag").matcher(home.getBody());
        while (matcherPage.find()) {
            list.add(Integer.parseInt(matcherPage.group(1)));
        }

        //排序
        Collections.sort(list);
        int maxPage;
        if (list.size()>1){
            maxPage = list.get(list.size() - 1);
        }else {
            maxPage=1;
        }



        ExecutorService pool=new ThreadPoolExecutor(
                3,
                5,
                6, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(5),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy());


        System.out.println(Thread.currentThread().getName() + "任务开始");



            for (int i = 1; i <= maxPage; i++) {
                final int page=i;
                //分页下载
                pool.execute(() ->{


                    //aid集合
                    Set<String> aid = new CopyOnWriteArraySet<>();
                    HttpResponse<String> item = Unirest.get("https://www.wnacg.com/albums-index-page-" + page + "-tag-" + name + ".html").asString();

                    Matcher matcher = Pattern.compile("aid-(\\d+).html").matcher(item.getBody());
                    while (matcher.find()) {
                        aid.add(matcher.group(1));
                    }

                    System.out.println("线程"+Thread.currentThread().getName()+"：正在添加第"+page+"页");
                    for (String s : aid) {
                        String urlNew = "https://www.wnacg.com/download-index-aid-" + s + ".html";
                        HttpResponse<String> home1 = Unirest.get(urlNew).asString();

                        Matcher matcherUrl = Pattern.compile("<a class=\"down_btn ads\" href=\"//(.+話|[完结])\">").matcher(home1.getBody());

                        while (matcherUrl.find()) {
                            down.add(matcherUrl.group(1));
                            int i1 = count.incrementAndGet();
                            System.out.println(i1+".正在获取"+matcherUrl.group(1).substring(61));

                        }
                    }



                });


            }



        pool.shutdown();

        while (!pool.isTerminated()) {

        }
   //文件输出
        try {
            FileOutputStream fos = new FileOutputStream(name+"_url.txt", true);
            PrintStream ps = new PrintStream(fos);
            System.setOut(ps);

            for (String s1 : down) {
                System.out.println("https://" + s1);
            }

            ps.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    }

