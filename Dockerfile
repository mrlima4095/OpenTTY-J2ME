FROM debian:13-slim
WORKDIR /app

RUN echo "deb http://deb.debian.org/debian trixie main" > /etc/apt/sources.list

# Install dependencies
RUN apt-get update && apt-get install -y --no-install-recommends python3 python3-pip python3-numpy python3-dev build-essential pkg-config  git gcc libpq-dev libxml2-dev libxslt1-dev ffmpeg

# Clean apt cache
RUN apt-get clean
RUN rm -rf /var/lib/apt/lists/*
RUN rm -rf /var/cache/apt/*.bin
 

RUN ln -s /usr/bin/python3 /usr/bin/python
RUN rm /usr/lib/python3.*/EXTERNALLY-MANAGED

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

ENTRYPOINT ["python", "app.py"]

EXPOSE 2048
EXPOSE 4095
VOLUME /app/uploads
VOLUME /app/data