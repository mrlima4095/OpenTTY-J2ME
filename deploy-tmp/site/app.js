(function () {
  "use strict";

  var clock = document.getElementById("clock");
  var status = document.getElementById("status");

  function pad(n) {
    return n < 10 ? "0" + n : String(n);
  }

  function tick() {
    var now = new Date();
    if (clock) {
      clock.textContent = pad(now.getHours()) + ":" + pad(now.getMinutes()) +
        ":" + pad(now.getSeconds());
    }
  }

  tick();
  setInterval(tick, 1000);

  // /nginx-status is a plain text page served by the app itself.
  if (status && window.fetch) {
    fetch("/nginx-status", { cache: "no-store" })
      .then(function (response) { return response.text(); })
      .then(function (text) { status.textContent = text; })
      .catch(function (error) { status.textContent = "status unavailable: " + error; });
  }
})();
