clc;
clear;

fid = fopen('s1.pcm16', 'r');
s1 = fread(fid,'int16');
fclose(fid);

plot(conv(s1(end:-1:1),s1));