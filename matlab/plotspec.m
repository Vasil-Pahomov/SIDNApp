function plotspec(data, maxkhz)
Fs=1e6; %������� ������������� ��� ������������ ������� ������
FPlotMax=1e3*maxkhz;
Skip=10; % ������������ ������ ������ Skip-��� ����� ������������� �������
Average=50; % ���� ��� ���������� ������� ����� ����������
maxtime=max(data.time);
datauni=resample(data,0:1/Fs:maxtime);
datauni=squeeze(datauni.data);
NFFT=2^nextpow2(length(datauni));
spec=abs(fft(datauni,NFFT));
freqs=Fs/2*linspace(0,1,NFFT/2+1)/1e3;
cutIdx=round(length(freqs)/(Fs/2)*FPlotMax);
filterb=ones(1,Average)/Average;
figure;plot(freqs(1:Skip:cutIdx),mag2db(filter(filterb, 1, spec(1:Skip:cutIdx))));
axis tight;
xlabel('���');
ylabel('��');