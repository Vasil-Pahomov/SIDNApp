clear;
clc;


fs=44100;%sampling freq, Hz
fc=21000;%base carrier freq, Hz
fcd=100;%carrier freq delta
SC=5;%number of carrier periods ber 1 bit (scale factor)
N=50;%sequence repetitions

load 'filt_hp_20kHz_44100'

%df=1E-3;%frequency difference
%dph=1E-2;%phase noise
noise=0;%noise level

%first signal
s=cacode(1);%only for O=10!
sc_ref=bpsk(s, fs, fc, SC, 0);
if (exist('Hd','var')); sc_ref=filter(Hd,sc_ref); end
sc_ref=sc_ref/max(sc_ref)*0.99;
sc_refq=bpsk(s, fs, fc, SC, 1);
if (exist('Hd','var')); sc_refq=filter(Hd,sc_refq); end
sc_refq=sc_refq/max(sc_refq)*0.99;


%second signal 
s2=cacode(2);
sc_ref2=bpsk(s2, fs, fc+fcd, SC, 0);
if (exist('Hd','var')); sc_ref2=filter(Hd,sc_ref2); end
sc_ref2=sc_ref2/max(sc_ref2)*0.99;
sc_ref2q=bpsk(s2, fs, fc+fcd, SC, 1);
if (exist('Hd','var')); sc_ref2q=filter(Hd,sc_ref2q); end
sc_ref2q=sc_ref2q/max(sc_ref2q)*0.99;


%align signals in length (pad zeros for short signals)
sclen = max([length(sc_ref) length(sc_ref2)]);
sc_ref = padarray(sc_ref,[0 sclen-length(sc_ref)],'post');
sc_ref2 = padarray(sc_ref2,[0 sclen-length(sc_ref2)],'post');

%%%%%saving kernels
fid = fopen('s1.pcm16', 'w');
s1=int16(sc_ref(end:-1:1)*32767);
fwrite(fid, s1,'int16');
fclose(fid);

fid = fopen('s2.pcm16', 'w');
s2=int16(sc_ref2(end:-1:1)*32767);
fwrite(fid, s2,'int16');
fclose(fid);

%repeat and mix signals
sc1=repmat(sc_ref,1,N);
sc1=sc1/max(sc1)*0.99;
sc2=repmat(sc_ref2,1,N);
sc2=sc2/max(sc2)*0.99;

ND=floor((fs/fc)*512*SC);%amount of samples to displace second signal, 0 for no displacement
sc = sc1 + circshift(sc2, ND);


%%%%%adding noise
scn=sc+randn(1,length(sc))*std(sc)*noise;
%%%%%playing
sound([sc1;sc2],fs);
%%%%%saving beacon signal
audiowrite('beacons.wav', [sc1' sc2'], fs);
%audiowrite('beacons.wav', (sc1'+sc2')/2, fs);

%[scn,fs]=audioread('rec2.wav');
%scn=scn(:,1)';

%reading from mobile (for comparing convolutions)
%dd=csvread('data.csv');
%scn=dd(1,:);

%%%%%plotting spectrum
S=abs(fft(scn));
S=S(1:floor(length(S)/2));
f=(0:(fs/length(S)):fs*(1-1/length(S)))/2;
figure(1);plot(f/1E3,S);title('src spectrum');

%%%%%%decoding the whole coded signal with matched filter

sf1=conv(sc_ref(end:-1:1),scn);
sf2=conv(sc_ref2(end:-1:1),scn);

%%%drawing
t1=0:1/fs:((length(sf1)-1)/fs);
t2=0:1/fs:((length(sf2)-1)/fs);
figure(2);plot(t1,abs(sf1),'r');%hold on;plot(t,abs(sf2),'b');hold off;
flen = 50;%filter length (in samples)
fil=ones(1, flen)/flen;

sd1=filter(fil,1,abs(sf1));
sd2=filter(fil,1,abs(sf2));
figure(3);plot(t1,sd1,'r');hold on;plot(t2,sd2,'b');hold off;

%testing difference between matlab and mobile convolution
%sf1=sf1./max(sf1);
%sfm1=dd(2,:)./max(dd(2,:));
%sf1len = min([length(sf1) length(sfm1)]);
%sf1=sf1(1:sf1len);
%sfm1=sfm1(1:sf1len);

%figure(4);plot(sf1-sfm1);