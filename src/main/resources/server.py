import SimpleHTTPServer
import SocketServer

PORT = 8000


class MyRequestHandler(SimpleHTTPServer.SimpleHTTPRequestHandler):
    def do_GET(self):
        ema = self.path.split('=')[1]
        text_file = open("Output.txt", "a+")
        text_file.write(ema + "\r\n")
        text_file.close()

        self._set_headers()
        self.wfile.write("")

    def _set_headers(self):
        self.send_response(200)
        self.send_header('Content-type', 'text/html')
        self.end_headers()


if __name__== "__main__":
    Handler = MyRequestHandler

    httpd = SocketServer.TCPServer(("", PORT), Handler)

    print "serving at port", PORT
    httpd.serve_forever()
