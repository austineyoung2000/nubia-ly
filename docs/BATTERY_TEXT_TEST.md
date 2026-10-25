# Battery percentage text color test

Signed test build 1.1.6 adds **Status bar → Colors → Battery percentage text** alongside Battery icon. Both use the existing color picker and Native reset. Apply uses the existing guarded status-bar activation and Restore contract.

Test on NX809J:
- Pick a magenta Battery icon and white Battery percentage text, then Apply.
- Check inside and outside percentage positions, Text only, and the QS header. Change level/theme and reopen QS; the colors should remain independent.
- Set QS Header status icon color to a third color. Battery icon and percentage colors must remain independent in compact and expanded QS; the other header icons still use the header color. Native battery colors fall back to the header color when selected.
- Try circular and filled battery designs; the percentage should keep its selected color. Charging symbols continue to use their existing appearance.
- Set percentage text to Native and Apply; then Restore Colors. Stock text/icon colors should return, and unrelated settings should remain.

Old palettes decode with Native percentage text. Color rendering uses scoped draw layers and passes the text color directly to replacement battery renderers; it does not overwrite native text colors or vendor battery state. Sidebar layout and sizing are unchanged.
