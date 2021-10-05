package fusy.graphic;

import bricks.graphic.BluntLineBrick;
import bricks.graphic.CircleBrick;
import bricks.graphic.RectangleBrick;
import bricks.graphic.TextBrick;
import bricks.trade.Host;

import java.util.List;

public abstract class Graphic extends airbricks.Wall {
    protected void load(Object ... l) {
        $bricks.setEntire(List.of(l));
    }

    protected class Rectangle extends RectangleBrick {

        public Rectangle() {
            super(Graphic.this);
        }
    }

    protected class Circle extends CircleBrick {

        public Circle() {
            super(Graphic.this);
        }
    }

    protected class Line extends BluntLineBrick {

        public Line() {
            super(Graphic.this);
        }
    }

    protected class Text extends TextBrick {

        public Text() {
            super(Graphic.this);
        }
    }
}
