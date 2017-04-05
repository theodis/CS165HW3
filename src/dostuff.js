function build2DArray(width, height) {
	ret = [];
	for(j = 0; j < height; j++){
		row = [];
		for(i = 0; i < width; i++){
			row.push(0);
		}
		ret.push(row);
	}

	return ret;
}

function sample(arr, x, y) {
	height = arr.length;
	width = arr[0].length;
	while(x >= width) x -= width;
	while(x < 0) x += width;
	while(y >= height) y -= height;
	while(y < 0) y += height;

	return arr[y][x];
}

function setSample(arr, x, y, value) {
	height = arr.length;
	width = arr[0].length;
	while(x >= width) x -= width;
	while(x < 0) x += width;
	while(y >= height) y -= height;
	while(y < 0) y += height;

	return arr[y][x] = value;
}

function sampleSquare(arr, x, y, size, value) {
	hs = Math.floor(size / 2);
	a = sample(arr, x - hs, y - hs);
	b = sample(arr, x + hs, y - hs);
	c = sample(arr, x - hs, y + hs);
	d = sample(arr, x + hs, y + hs);

	setSample(arr,x,y, ((a + b + c + d) / 4) + value);
}

function sampleDiamond(arr, x, y, size, value) {
	hs = Math.floor(size / 2);
	a = sample(arr, x - hs, y);
	b = sample(arr, x + hs, y);
	c = sample(arr, x, y - hs);
	d = sample(arr, x, y + hs);

	setSample(arr,x,y, ((a + b + c + d) / 4) + value);
}

function DiamondSquare(arr, width, height, stepsize, scale) {
	halfstep = Math.floor( stepsize / 2 );

	for(y = halfstep; y < height + halfstep; y += stepsize){
		for(x = halfstep; x < width + halfstep; x += stepsize){
			sampleSquare(arr, x, y, stepsize, Math.random() * scale);
		}
	}

	for(y = halfstep; y < height; y += stepsize){
		for(x = halfstep; x < width; x += stepsize){
			sampleDiamond(arr, x + halfstep, y, stepsize, Math.random() * scale);
			sampleDiamond(arr, x, y + halfstep, stepsize, Math.random() * scale);
		}
	}
}

function MidpointDisplacement(size, scale) {
	ret = build2DArray(size, size);

	sampleSize = size;

	while(sampleSize > 1){
		DiamondSquare(ret, size, size, sampleSize, scale);
		sampleSize = Math.floor(sampleSize / 2);
		scale /= 2;
	}

	return ret;
}

a = MidpointDisplacement(129,16);
