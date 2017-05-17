package a3;

import sage.event.*;

public class TankDestroyedEvent extends AbstractGameEvent {
		private Tank source;
		private Tank destroyed;
	
		public TankDestroyedEvent(Tank s, Tank d){
			source = s;
			destroyed = d;
		}
		
		public Tank getSource() {return source;}
		public Tank getDestroyed() {return destroyed;}
}