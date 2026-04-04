(function () {
  var KEY = "riceThemePreference";

  function applyTheme(theme) {
    document.documentElement.setAttribute(
      "data-theme",
      theme === "light" ? "light" : "dark"
    );
  }

  window.addEventListener("storage", function (e) {
    if (e.key !== KEY || e.newValue == null) return;
    applyTheme(e.newValue);
  });

  document.addEventListener("DOMContentLoaded", function () {
    var btn = document.getElementById("rice-theme-toggle");
    if (!btn) return;

    btn.addEventListener("click", function () {
      var current = document.documentElement.getAttribute("data-theme");
      var next = current === "light" ? "dark" : "light";
      applyTheme(next);
      try {
        localStorage.setItem(KEY, next);
      } catch (e) {}

      btn.classList.remove("rice-theme-spinning");
      void btn.offsetWidth;
      btn.classList.add("rice-theme-spinning");
      setTimeout(function () {
        btn.classList.remove("rice-theme-spinning");
      }, 500);
    });
  });
})();
