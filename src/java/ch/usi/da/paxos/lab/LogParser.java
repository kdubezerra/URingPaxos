package ch.usi.da.paxos.lab;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LogParser {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		File file = new File(args[0]);
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String l;
		List<Float> msgl = new ArrayList<Float>();
		List<Float> mbitl = new ArrayList<Float>();
		List<Float> avgl = new ArrayList<Float>();
		List<Float> nimsgl = new ArrayList<Float>();
		List<Float> nimbitl = new ArrayList<Float>();
		List<Float> niavgl = new ArrayList<Float>();
		List<Float> nomsgl = new ArrayList<Float>();
		List<Float> nombitl = new ArrayList<Float>();
		List<Float> noavgl = new ArrayList<Float>();
		String latency = null;
		while((l = reader.readLine()) != null){
			String[] xs = l.split(" ");
			if(l.contains("delivered")){ // learner throughput
				float msg = Float.parseFloat(xs[8]);
				float mbit= Float.parseFloat(xs[10]);
				float avg = Float.parseFloat(xs[13]);
				if(avg > 0 && avg < 2000){ // remove first value
					msgl.add(msg);
					mbitl.add(mbit);
					avgl.add(avg);
					//System.out.println(msg + "," + mbit + "," + avg);
				}
			}else if(l.contains("TCP")){ // TCP
				float nimsg = Float.parseFloat(xs[8].split("/")[0]);
				float nimbit= Float.parseFloat(xs[10].split("/")[0]);
				float niavg = Float.parseFloat(xs[13].split("/")[0]);
				float nomsg = Float.parseFloat(xs[8].split("/")[1]);
				float nombit= Float.parseFloat(xs[10].split("/")[1]);
				float noavg = Float.parseFloat(xs[13].split("/")[1]);
				if(niavg > 0 && niavg < 2000){ // remove first value
					nimsgl.add(nimsg);
					nimbitl.add(nimbit);
					niavgl.add(niavg);
					nomsgl.add(nomsg);
					nombitl.add(nombit);
					noavgl.add(noavg);
					//System.out.println(nomsg + "," + nombit + "," + noavg);
				}
			}else if (l.contains("latency")){
				latency = l.substring(38);
				break;
			}
		}
		if(msgl.size()>0){
			msgl.remove(msgl.size()-1);
			mbitl.remove(mbitl.size()-1);
			avgl.remove(avgl.size()-1);
			System.out.println(file.getAbsolutePath() + " " + avg(msgl) + " " + avg(mbitl) + " " + avgl.get(avgl.size()-1) + " " + (avgl.size()*5) + "s learner");			
		}
		if(nimsgl.size()>0){
			nimsgl.remove(nimsgl.size()-1);
			nimbitl.remove(nimbitl.size()-1);
			niavgl.remove(niavgl.size()-1);
			nomsgl.remove(nomsgl.size()-1);
			nombitl.remove(nombitl.size()-1);
			noavgl.remove(noavgl.size()-1);
			System.out.println(file.getAbsolutePath() + " " + avg(nimsgl) + " " + avg(nimbitl) + " " + niavgl.get(niavgl.size()-1) + " " + (niavgl.size()*5) + "s TCP IN");
			System.out.println(file.getAbsolutePath() + " " + avg(nomsgl) + " " + avg(nombitl) + " " + noavgl.get(noavgl.size()-1) + " " + (noavgl.size()*5) + "s TCP OUT");
		}
		if(latency != null){
			System.out.println(file.getAbsolutePath() + " " + latency);
		}
		reader.close();
	}

	public static Float avg(List<Float> l){
		float a = 0;
		for(Float f : l){
			a = a + f;
		}
		return (float)a/l.size();
	}
}
