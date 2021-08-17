using Toybox.WatchUi;
using Track;

class TrackView extends GenericView {

    var zoomLevel = null;
    var scaleFactor;
    var refScale = 2.0;
    const SCALE_PIXEL = 0.1;
    const SCALES = [12.0, 20.0, 30.0, 50.0, 80.0,
                    120.0, 200.0, 300.0, 500.0, 800.0,
                    1200.0, 2000.0, 3000.0, 5000.0, 8000.0,
                    12000.0, 20000.0, 30000.0, 50000.0, 80000.0,
                    120000.0, 200000.0, 300000.0, 500000.0, 800000.0];

    // used for drawing track

    var pixelHeight;
    var pixelWidth;
    var pixelWidth2;
    var pixelHeight2;
    var pixelHeight3;
    var pixelMin;

    var xs_center;
    var ys_center;

    var scale_x1;
    var scale_y1;
    var scale_x2;
    var scale_y2;
    var compass_x;
    var compass_y;
    var compass_size;

    var cursorSizePixel;
    var posCursor;
    var isNewTrack=false;
    var activity_values;
    var fontsize = Graphics.FONT_MEDIUM;

    //var fontsizeNumber = Graphics.FONT_NUMBER_MILD;
    var fontsizeNumber = Graphics.FONT_LARGE;
    var topPadding = 0.0;
    var bottomPadding = 0.0;

    function setPixelDimensions(width, height) {
        pixelWidth = width;
        pixelWidth2 = 0.5 * pixelWidth;
        pixelHeight = height;
        pixelHeight2 = 0.5 * pixelHeight;
        pixelHeight3 = 0.6666667 * pixelHeight;
        pixelMin = pixelWidth < pixelHeight ? pixelWidth : pixelHeight;
        scale_x1 = pixelWidth * (0.5 - SCALE_PIXEL);
        scale_y1 = (1.0 - 0.45 * SCALE_PIXEL) * pixelHeight;
        scale_y2 = (1.0 - 0.2 * SCALE_PIXEL) * pixelHeight;
        scale_x2 = pixelWidth * (0.5 + SCALE_PIXEL);
        compass_size = 0.25 * (scale_x2 - scale_x1);
        compass_x = scale_x2 + 2 * compass_size;
        compass_y = scale_y2 - compass_size;
    }


    function drawScale(dc) {
        dc.setColor(foregroundColor, Graphics.COLOR_TRANSPARENT);
        dc.setPenWidth(2);

        dc.drawLine(scale_x1, scale_y1 - bottomPadding,
                    scale_x1, scale_y2 - bottomPadding);
        dc.drawLine(scale_x1, scale_y2 - bottomPadding,
                    scale_x2, scale_y2 - bottomPadding);
        dc.drawLine(scale_x2, scale_y2 - bottomPadding,
                    scale_x2, scale_y1 - bottomPadding);
        dc.drawText(pixelWidth2, scale_y2 - dc.getFontHeight(fontsize) - bottomPadding,
            fontsize , formattedScale(), Graphics.TEXT_JUSTIFY_CENTER);

    }

    function drawActivityInfo(dc) {
        dc.setColor(foregroundColor, Graphics.COLOR_TRANSPARENT);
        var actualFontsize = $.trackViewLargeFont ? fontsizeNumber : fontsize;
        var y = 0.5*dc.getFontAscent(actualFontsize);
        for(var i = 0; i < Data.getField(3, 0); i++) {
            // index of data field
            var j = Data.getField(3, i+1);
            dc.drawText(pixelWidth2, topPadding + (1+2*i)*y,
                actualFontsize,
                //Data.dataFieldSLabels[j] +": "+
                Data.getDataFieldLabelValue(j)[1], Graphics.TEXT_JUSTIFY_CENTER | Graphics.TEXT_JUSTIFY_VCENTER);
        }

    }

    function drawTrack(dc) {

        // center of screen in x,y coordinates
        var xc = Track.xPos;
        var yc = Track.yPos;
        if(Track.centerMap) {
            // center of map is defined by (0,0) by construction
            xc = 0.0;
            yc = 0.0;
        }

        xs_center = pixelWidth2;
        ys_center = pixelHeight2;
        var cos_heading_smooth = 1.0;
        var sin_heading_smooth = 0.0;
        if( !Track.northHeading && !Track.centerMap) {
            cos_heading_smooth = Track.cos_heading_smooth;
            sin_heading_smooth = Track.sin_heading_smooth;
            ys_center = pixelHeight3;
        }

        var x1 = 0.0;
        var y1 = 0.0;
        var x2 = 0.0;
        var y2 = 0.0;
        var xr = 0.0;
        var yr = 0.0;
        var xya = null;

        var d2 = Track.EARTH_RADIUS * Track.EARTH_RADIUS;
        var dxy2 = 0.0;
        var xp = Track.xPos;
        var yp = Track.yPos;
        var nearestPointIndex = -1;
        var nearestPointLambda = 0.0;
        var findNearestPoint = Track.onPositionCalled && Track.findNearestPoint;
        var dx = 0.0;
        var dy = 0.0;
        var xt1 = 0.0;
        var yt1 = 0.0;
        var xt2 = 0.0;
        var yt2 = 0.0;
        var xs = 0.0;
        var ys = 0.0;
        var ds = 0.0;
        var s = 0.0;

        if($.track != null) {
            dc.setColor(trackColor, Graphics.COLOR_TRANSPARENT);
            dc.setPenWidth(2);

            xya = $.track.xyArray;

            for(var i = -2; i < xya.size() - 3; i += 2 ) {
                if(i >= 0) {
                    x1 = x2;
                    y1 = y2;
                    xt1 = xt2;
                    yt1 = yt2;
                }
                xt2 = xya[i + 2];
                yt2 = xya[i + 3];
                xr = scaleFactor * (xt2 - xc);
                yr = scaleFactor * (yt2 - yc);
                x2 = xs_center + xr * cos_heading_smooth - yr * sin_heading_smooth;
                y2 = ys_center - xr * sin_heading_smooth - yr * cos_heading_smooth;

                if(i >= 0) {
                    dc.drawLine(x1, y1, x2, y2);

                    if(findNearestPoint) {
                        xs = xt2 - xt1;
                        ys = yt2 - yt1;
                        ds = xs * xs + ys * ys;
                        if(ds > 1.0e-12) {
                            s = (xs * (xp - xt1) + ys * (yp - yt1)) / ds;
                            if(s < 0.0) {
                                s = 0.0;
                            } else if(s > 1.0) {
                                s = 1.0;
                            }
                        } else {
                            s = 0.0;
                        }
                        dx = xp - (xt1 + s * xs);
                        dy = yp - (yt1 + s * ys);
                        dxy2 = dx * dx + dy * dy;
                        if(dxy2 < d2) {
                            nearestPointIndex = i;
                            nearestPointLambda = s;
                            d2 = dxy2;
                        }
                    }
                }
            }
        }

        // draw nearest point on track
        if(findNearestPoint && nearestPointIndex >= 0) {
            dc.setColor(Graphics.COLOR_BLUE, Graphics.COLOR_TRANSPARENT);
            x1 = xya[nearestPointIndex] + nearestPointLambda * (xya[nearestPointIndex + 2] -  xya[nearestPointIndex]);
            y1 = xya[nearestPointIndex + 1] + nearestPointLambda * (xya[nearestPointIndex + 3] -  xya[nearestPointIndex + 1]);
            var xy_pos = xy_2_screen(x1, y1);
            dc.fillCircle(xy_pos[0], xy_pos[1], 4);
        }

        // draw breadcrumbs
        if(Track.pos_nelements > 0) {
            dc.setColor(Graphics.COLOR_DK_GREEN, Graphics.COLOR_TRANSPARENT);
            // (x,y) coordinates of recorded breadcrumb points
            xya = Track.bxy;
            for(var i = 0; i < Track.pos_nelements; i += 1) {
                var j = (Track.pos_start_index + i) % Track.breadCrumbNumber;
                xr = scaleFactor * (xya[2 * j] - xc);
                yr = scaleFactor * (xya[2 * j + 1] - yc);
                dc.fillCircle(xs_center + xr * cos_heading_smooth - yr * sin_heading_smooth,
                              ys_center - xr * sin_heading_smooth - yr * cos_heading_smooth, 4);
            }
        }
    }

    function drawPositionArrowAndCompass(dc) {

        dc.setColor(Graphics.COLOR_BLUE, Graphics.COLOR_TRANSPARENT);
        dc.setPenWidth(1);

        var sinHead =Track.sin_heading_smooth;
        var cosHead =Track.cos_heading_smooth;

        //var heading;
        var dx1;
        var dy1;
        var dx2;
        var dy2;
        var dx3;
        var dy3;

        if(Track.northHeading || Track.centerMap) {
            dx1 =  cursorSizePixel * sinHead;
            dy1 = -cursorSizePixel * cosHead;
            dx2 =  cursorSizePixel * (cosHead - 1.577352 * sinHead);
            dy2 =  cursorSizePixel * (sinHead + 1.577352 * cosHead);
        } else {
            dx1 = 0.0;
            dy1 = -cursorSizePixel;
            dx2 = cursorSizePixel;
            dy2 = 1.577352 * cursorSizePixel;
        }
        dx3 = -3.154704 * dx1 - dx2;
        dy3 = -3.154704 * dy1 - dy2;

        var xy_pos = xy_2_screen(Track.xPos, Track.yPos);

        dc.setPenWidth(3);
        var x1 = xy_pos[0] + dx1;
        var y1 = xy_pos[1] + dy1;
        var x2 = xy_pos[0] + dx2;
        var y2 = xy_pos[1] + dy2;
        var x3 = xy_pos[0] - dx1;
        var y3 = xy_pos[1] - dy1;
        var x4 = xy_pos[0] + dx3;
        var y4 = xy_pos[1] + dy3;

        dc.drawLine(x1,y1,x2,y2);
        dc.drawLine(x2,y2,x3,y3);
        dc.drawLine(x3,y3,x4,y4);
        dc.drawLine(x4,y4,x1,y1);

        dc.setColor(backgroundColor, backgroundColor);
        dc.fillCircle(compass_x, compass_y, compass_size);

        if(Track.northHeading || Track.centerMap) {
            dx1 = - 0.5 * compass_size;
            dx2 = 0.0;
            dx3 = -dx1;
            dy1 = 0.0;
            dy2 = - compass_size;
            dy3 = 0.0;
        } else {
            dx1 = - 0.5 * compass_size * cosHead;
            dy1 = + 0.5 * compass_size * sinHead;
            dx2 = - compass_size * sinHead;
            dy2 = - compass_size * cosHead;
            dx3 = - dx1;
            dy3 = - dy1;
        }

        dc.setColor(Graphics.COLOR_BLUE, Graphics.COLOR_TRANSPARENT);
        var points = [[compass_x + dx1, compass_y + dy1 - bottomPadding],
                      [compass_x - dx2, compass_y - dy2 - bottomPadding],
                      [compass_x + dx3, compass_y + dy3 - bottomPadding]];
        dc.fillPolygon(points);

        dc.setColor(Graphics.COLOR_RED, Graphics.COLOR_TRANSPARENT);
        points[1] = [compass_x + dx2, compass_y + dy2 - bottomPadding];
        dc.fillPolygon(points);

    }

    function initialize() {
        GenericView.initialize();
        if($.device.equals("vivoactive")) {
            fontsize=Graphics.FONT_XTINY;
        }
        Track.reset();
        activity_values = new[2];
        setDarkMode($.isDarkMode);
    }

    function xy_2_screen(x, y) {
        // center of screen in x,y coordinates
        var xc = Track.xPos;
        var yc = Track.yPos;
        if(Track.centerMap) {
            // center of map is defined by (0,0) by construction
            xc = 0.0;
            yc = 0.0;
        }

        var xr = scaleFactor * (x - xc);
        var yr = scaleFactor * (y - yc);
        if(Track.northHeading || Track.centerMap || !Track.onPositionCalled) {
            return [xs_center + xr, ys_center - yr];
        } else {
            return [xs_center + xr * Track.cos_heading_smooth - yr * Track.sin_heading_smooth,
                    ys_center - xr * Track.sin_heading_smooth - yr * Track.cos_heading_smooth];
        }
    }

    // Load your resources here
    function onLayout(dc) {
        //System.println("onLayout(dc)");
        setPixelDimensions(dc.getWidth(), dc.getHeight());
        cursorSizePixel = pixelWidth * SCALE_PIXEL * 0.5;
        if($.device.equals("vivoactive")) {
            topPadding = 0.5*dc.getFontAscent(fontsize);
            bottomPadding = 0.5*dc.getFontAscent(fontsize);
        }
    }

    // Called when this View is brought to the foreground. Restore
    // the state of this View and prepare it to be shown. This includes
    // loading resources into memory.
    function onShow() {
        //System.println("onShow()");
        View.onShow();
        // inital zoom level when no track is loaded
        if(zoomLevel == null && $.track == null) {
            setZoomLevel(5);
        }
    }

    // Update the view
    function onUpdate(dc) {
        // Call the parent onUpdate function to redraw the layout
        //View.onUpdate(dc);
        dc.setColor(backgroundColor, backgroundColor);
        dc.clear();

        if(isNewTrack && $.track != null) {
            isNewTrack = false;
            Track.reset();
            Track.newTrack();
            setZoomLevel(null);
        }

        drawTrack(dc);

        if(Track.xPos != null) {
            drawPositionArrowAndCompass(dc);
        }

        if(session != null) {
            drawActivityInfo(dc);
        }

        drawScale(dc);
        drawStartStop(dc);

    }

    function setZoomLevel(l) {
        if(l == null) {
            // fit to track size
            zoomLevel = 0;
            for(zoomLevel= 0; zoomLevel < SCALES.size(); zoomLevel += 1 ) {
                if(pixelMin / (0.2 * pixelWidth)* SCALES[zoomLevel] * 0.95 > $.track.diagonal) {
                    break;
                }
            }
        } else {
            if(l == -1 && zoomLevel > 0) {
                zoomLevel -=1;
            } else if(l == -2 && zoomLevel < SCALES.size() - 1) {
                zoomLevel += 1;
            } else if(l >=0 && l <= SCALES.size()) {
                zoomLevel = l;
            }
        }
        refScale = SCALES[zoomLevel];
        scaleFactor = 0.2 * pixelWidth / refScale * Track.EARTH_RADIUS;
        return zoomLevel;
    }


    function formattedScale() {
        return (refScale < 1000.0) ? refScale.format("%d") + "m" : (0.001 * refScale).format("%.1f") + "k";
    }

}
