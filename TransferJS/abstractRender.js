var vertex_src =
"      attribute vec3 aVertexPosition;" +
"      attribute vec2 tex_coord;" + 
"      varying highp vec2 vtex_coord;" + 
"      void main() {" +
"        vtex_coord = tex_coord;" +
"        gl_Position = vec4(aVertexPosition, 1.0);" +
"      }";

var gl = null;

var errors = [];

function checkgl() {
    var e = gl.getError();
    if(e) console.error("WebGL produced error " + errors[e] + " (" + e + ")");
}

function start() {
    var canvas = document.getElementById("glcanvas");
    gl = initWebGL(canvas);
}

function initBuffers() {
    vertices = [
       -1.0, -1.0, 0.0,
        1.0, -1.0, 0.0,
        1.0,  1.0, 0.0,
       -1.0,  1.0, 0.0
    ];

    tcoords = [
        0, 0,
        1, 0,
        1, 1,
        0, 1
    ];

    vbuf = gl.createBuffer();
    checkgl();
    gl.bindBuffer(gl.ARRAY_BUFFER, vbuf);
    checkgl();
    gl.bufferData(gl.ARRAY_BUFFER, new Float32Array(vertices),
                  gl.STATIC_DRAW);
    checkgl();
    tbuf = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, tbuf);
    gl.bufferData(gl.ARRAY_BUFFER, new Float32Array(tcoords),
                  gl.STATIC_DRAW);
    checkgl();
}

function setParam(name) {
    var attrib = gl.getUniformLocation(prog, name);
    var el = document.getElementById(name);
    var v = parseFloat(el.value);
    if($(el).hasClass("flipped"))
        v = parseFloat(el.max) - v;
    gl.uniform1f(attrib, v);
    checkgl();
}

function draw() {
    // set the shader parameters
    $('input.param').each(function (i, p) {
        setParam(p.id);
    });

    // Actually draw
    gl.clearColor(0, 0, 0, 1.0);
    gl.clear(gl.COLOR_BUFFER_BIT);

    gl.activeTexture(gl.TEXTURE0);
    gl.bindTexture(gl.TEXTURE_2D, tx);
    tex_attr = gl.getUniformLocation(prog, "tex")
    gl.uniform1i(tex_attr, 0);
    
    checkgl();
    
    // DRAW!
    gl.bindBuffer(gl.ARRAY_BUFFER, vbuf);
    gl.vertexAttribPointer(vattr, 3, gl.FLOAT, false, 0, 0);
    checkgl();
    gl.bindBuffer(gl.ARRAY_BUFFER, tbuf);
    gl.vertexAttribPointer(tattr, 2, gl.FLOAT, false, 0, 0);
    checkgl();
    //gl.disableVertexAttribArray(tattr);
    gl.drawArrays(gl.TRIANGLE_FAN, 0, 4);
    checkgl();
}

function reformat(data) {
  //For each item in data, turn it into a vec4 that can be recase to the original int
  //Also observes min and max values in the dataset

  var rslt = new Uint8Array(data.length*4);
  var minv = data[0];
  var maxv = data[0];

  for(var i=0; i<data.length;i++) {
    var item = data[i];
    var c1 = item%256;
    var c2 = (item>>8)%256;
    var c3 = (item>>16)%256;
    var c4 = (item>>24)%256;
    rslt[i*4]=c1;
    rslt[i*4+1]=c2;
    rslt[i*4+2]=c3;
    rslt[i*4+3]=c4;

    if (item > maxv) {maxv = item;}
    if (item < minv) {minv = item;}
  }
  return { "min": minv, "max": maxv, "aggs": rslt };
  
}

function loadTexture(aggsFile) {
  $.ajax(aggsFile, { success: function(info) {
    if (typeof(info) == "string") {info = JSON.parse(info)}
    var width = info.width;
    var height = info.aggs.length/width;
    var rslt = reformat(info.aggs);
    var aggs = rslt.aggs;
    var minv = rslt.min;
    var maxv = rslt.max;

    //console.info(width);
    //console.info(height);

    var minl = gl.getUniformLocation(prog, "minv") 
    var maxl = gl.getUniformLocation(prog, "maxv")
    gl.uniform1f(minl, minv);
    gl.uniform1f(maxl, maxv);
    
    tx = gl.createTexture();
    gl.bindTexture(gl.TEXTURE_2D, tx);
    checkgl();
    gl.pixelStorei(gl.UNPACK_FLIP_Y_WEBGL, true);
    
    gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, width, height, 0, gl.RGBA,
      gl.UNSIGNED_BYTE, new Uint8Array(aggs));
    //gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.INT32, aggs);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.NEAREST);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.NEAREST);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
    checkgl();

    console.info("Texture loaded, about to draw...");

    draw();
  }});
}

function readContents(iframe) {
    return iframe.document.getElementsByTagName("pre")[0].innerText;
}

function initWebGL(canvas) {
    try {
        gl = canvas.getContext("experimental-webgl");
    }
    catch(e) {
        console.error("Error getting context.");
    }

    if(!gl) {
        console.error("Your browser doesn't appear to support WebGL.");
    }

    // initialize error strings
    for(x in gl) {
        errors[gl[x]] = x;
    }

    // initialize shaders
    vshade = makeShader(vertex_src, gl.VERTEX_SHADER);
    $.ajax({
        url: "transfer.txt",
        success: function(frag_src) {
            fshade = makeShader(frag_src, gl.FRAGMENT_SHADER);
            prog = gl.createProgram();
            
            gl.attachShader(prog, vshade);
            gl.attachShader(prog, fshade);
            gl.linkProgram(prog);
            
            if (!gl.getProgramParameter(prog, gl.LINK_STATUS)) {
                console.error("Unable to initialize the shader program.");
            }
            
            gl.useProgram(prog);
            
            // set up attributes
            vattr = gl.getAttribLocation(prog, "aVertexPosition");
            gl.enableVertexAttribArray(vattr);
            checkgl();
            
            tattr = gl.getAttribLocation(prog, "tex_coord");
            gl.enableVertexAttribArray(tattr);
            checkgl();
            
            initBuffers();
            
            console.info("Loaded fragment shader and initialized buffers.");
            loadTexture("aggregates.json");
        }
    });

    return gl;
}

function makeShader(src, type) {
    var shader = gl.createShader(type);
    checkgl();
    
    gl.shaderSource(shader, src);
    checkgl();
    
    gl.compileShader(shader);
    checkgl();
    
    if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {
        console.error("An error occurred compiling the shaders: "
                      + gl.getShaderInfoLog(shader));
        return null;
    }
    
    return shader;
}
