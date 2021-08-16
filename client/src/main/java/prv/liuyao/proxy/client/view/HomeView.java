package prv.liuyao.proxy.client.view;

import prv.liuyao.proxy.client.listener.ButListener;

import javax.swing.*;
import java.awt.*;

/**
 *  可视化界面
 * @author zhangpl
 * @description ViewLogin
 * @date 2021/8/13 16:24
 */
public class HomeView {

    public static void showUI(){
        //窗体类
        JFrame jf = new JFrame();
        //窗体名称
        jf.setTitle("VPN");
        //窗体大小（具体值跟电脑显示器的像素有关，可调整到合适大小）
        jf.setSize(500, 300);
        //设置退出进程的方法
        jf.setDefaultCloseOperation(3);
        //设置居中显示用3
        jf.setLocationRelativeTo(null);

        //流式布局管理器
        FlowLayout flow = new FlowLayout();
        jf.setLayout(flow);  //给窗体设置为流式布局——从左到右然后从上到下排列自己写的组件顺序

        //图片，冒号里是你存图片的地址
//        ImageIcon icon = new ImageIcon("bg.jpg");
//        //标签
//        JLabel jla = new JLabel(icon);
//        Dimension dm0=new Dimension(280,200);
//        //设置大小
//        jla.setPreferredSize(dm0);//应用大小到相应组件
//        jf.add(jla);//将组件加到窗体上

        //文本框
//        JTextField jtf = new JTextField();
//        Dimension dm = new Dimension(280, 30);
//        //(除了JFrame)其它所有组件设置大小都是该方法
//        jtf.setPreferredSize(dm);
//        jf.add(jtf);
//
//        JTextField jtf2=new JTextField();
//        Dimension dm2=new Dimension(280,30);
//        jtf2.setPreferredSize(dm2);
//        jf.add(jtf2);

        //复选框
//        JCheckBox jcb = new JCheckBox("记住密码");
//        jf.add(jcb);
//
//        JCheckBox jcb2 = new JCheckBox("忘记密码");
//        jf.add(jcb2);

        //按钮
        Dimension dm3=new Dimension(100,30);
        JButton jbu1 = new JButton("启动");
        jbu1.setPreferredSize(dm3);
        jbu1.setLocation(20,100);
        jbu1.setName("start");
        jf.add(jbu1);   //给窗体添加一个按钮对象


        JButton jbu2 = new JButton("关闭");
        jbu2.setPreferredSize(dm3);
        jbu2.setLocation(120,100);
        jbu2.setName("close");
        jf.add(jbu2);   //给窗体添加一个按钮对象


        //给按钮添加动作监听器方法
        ButListener but = new ButListener();
        //创建一个监听器
        jbu1.addActionListener(but);
        jbu2.addActionListener(but);
        //把监听器加在“登录”按钮上
//        but.setJt(jtf,jtf2);

        jf.setVisible(true);   //设置可见，放在代码最后一句
    }

    public static void main(String[] args) {
        HomeView lo = new HomeView();
        lo.showUI();
    }
}
