# Ledgerly Web Interface (Laptop Edition)

A high-productivity web application for **Ledgerly — Personal Finance Management**, offering 100% feature parity with the native Android application.

Built with **React 18 / 19**, **Vite**, **TypeScript**, **Tailwind CSS**, and **Supabase JS Client**.

---

## 🚀 Key Features (100% Mobile Parity)

1. **Dashboard & KPIs**:
   - Dual Wallet Cards (Expense Wallet, Savings Wallet, or Top Credit Card spend).
   - Daily Logging Streak counter (`🔥 X day streak!` or `✅ Logged today!`).
   - 90-Day Cost Projection summary card.
   - Monthly category budget caps with live progress bars.
   - Savings goals with visual targets and quick deposit/withdraw modals.
   - Recent transactions activity stream.

2. **Full 5-Tab Transaction Engine**:
   - **Personal Expense**: Amount, Category, Date/Time, Notes, Wallet selection (Expense, Credit Card, Custom Wallet), and **Interactive Expense Splitting** (equal split or custom split amounts among contacts and family members).
   - **Personal Income**: Inflows to Expense, Savings, or Custom Wallets.
   - **Family Transfer**: Direct transfers given/received with family members.
   - **Inter-Wallet Transfer**: Seamless money movement between Expense, Savings, Custom Wallets, and Credit Card repayments.
   - **Borrow / Lend**: Personal loans and debts with contacts and family members.

3. **Transactions Management Hub**:
   - Desktop data grid with sorting and instant search.
   - Advanced filters: Wallet, Transaction Type, Category, Family Member, Contact, and Date Range presets (Today, Week, Month, 90 Days).
   - Instant totals: Filtered Inflows vs Filtered Outflows.
   - Edit, delete, and **retroactive expense splitting**.
   - One-click export to **Excel (.xlsx)** or **CSV**.

4. **Wallets & Credit Cards Manager**:
   - Standard Wallets (Expense & Savings) with dynamic balance calculations.
   - Custom Event Wallets (create, customize color, rename, fund, and delete).
   - Credit Cards Hub (limits, statement spend, remaining credit, color themes, and one-click repayment).
   - **Borrow & Lend Ledger**: Debt tracking ("Owes you" vs "You owe"), contact management, and one-click **Settle Up** workflow.

5. **Family Hub**:
   - Directory of family members with relationship tags and avatars.
   - Financial relationship stats: Total given, Total received, and Net standing.
   - Direct transfer shortcuts and member transaction history.

6. **Estimation Insight & Analytics**:
   - 90-Day predictive monthly cost projection gauge (`totalSpending90Days / 3.0`).
   - Credit card velocity audit.
   - Calculation transparency breakdown.
   - 6-month cashflow trajectory chart & expense category breakdown donut chart.

7. **Settings & Subscriptions Scheduler**:
   - Custom Expense & Income category management.
   - Monthly budget spending caps per category.
   - Savings goals creator.
   - Recurring subscriptions & bills scheduler (Daily, Weekly, Monthly, Yearly).
   - Theme customizer: Light mode, Dark mode, Expense wallet gradient color, and Credit card theme color.
   - Supabase cloud connection diagnostic and manual sync triggers.

8. **User Profile**:
   - View and edit display name.
   - Secure password update via Supabase Auth.
   - Session logout.

9. **Multi-Table Excel Import Wizard**:
   - Bank statement & spreadsheet parsing (.xlsx / .xls).
   - Automatic multi-table and multi-sheet detection.
   - Per-table configuration (type, target wallet, category, card mapping).
   - Live editable preview table before batch database commit.

---

## 🛠️ Local Development

```bash
cd web
npm install
npm run dev
```

Open `http://localhost:5173` in your browser.

---

## 📦 Production Build

```bash
cd web
npm run build
```

This compiles a fast static Single Page Application into `web/dist/`.

---

## 🌐 Deploying to Netlify (Free Hosting)

Because Supabase handles the database, API, and authentication, **no backend server is required**. You can host the frontend on Netlify for free:

### Option A: GitHub Connection (Automatic CI/CD)
1. Push your repository to GitHub.
2. In [Netlify Dashboard](https://app.netlify.com/), click **Add new site** -> **Import an existing project**.
3. Choose your GitHub repository.
4. Set:
   - **Base directory**: `web`
   - **Build command**: `npm run build`
   - **Publish directory**: `dist`
5. Click **Deploy Site**!

### Option B: Netlify Drop (Zero Configuration)
1. In your terminal, run:
   ```bash
   cd web
   npm run build
   ```
2. Go to [app.netlify.com/drop](https://app.netlify.com/drop).
3. Drag and drop the `web/dist` folder into the browser window.
4. Your site is live instantly!

### Option C: Netlify CLI
```bash
cd web
npx netlify deploy --prod --dir=dist
```
